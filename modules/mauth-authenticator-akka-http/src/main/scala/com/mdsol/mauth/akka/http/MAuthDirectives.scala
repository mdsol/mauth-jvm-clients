package com.mdsol.mauth.akka.http

import java.security.PublicKey
import java.util.UUID

import akka.http.javadsl.server.AuthorizationFailedRejection
import akka.http.scaladsl.model.{HttpEntity, HttpRequest}
import akka.http.scaladsl.server.Directives.headerValueByType
import akka.http.scaladsl.server._
import akka.http.scaladsl.server.directives.BasicDirectives._
import akka.http.scaladsl.server.directives.FutureDirectives.onComplete
import akka.http.scaladsl.server.directives.RouteDirectives.reject
import akka.http.scaladsl.util.FastFuture
import com.mdsol.mauth.akka.http.MAuthSignatureEngine.{buildSignature, compareDigests}
import com.mdsol.mauth.http.{HttpVerbOps, X_MWS_Authentication, X_MWS_Time}
import com.mdsol.mauth.scaladsl.utils.ClientPublicKeyProvider
import com.typesafe.scalalogging.StrictLogging

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal
import scala.util.{Success, Try}

trait MAuthDirectives extends StrictLogging {

  case class AuthHeaderDetail(appId: UUID, hash: String)

  case object MdsolAuthFailedRejection
    extends AuthorizationFailedRejection with Rejection

  /**
    * Directive to wrap all routes that require MAuth authentication check.
    * Should only be used once per route branch, as any HttpEntity is forced
    * to be strict, and serialised into the request.
    *
    * @param kl      MAuth Public Key Provider
    * @param timeout request timeout duration
    * @return Directive to authenticate the request
    */
  def authenticate(implicit kl: ClientPublicKeyProvider, timeout: FiniteDuration): Directive0 = {
    extractExecutionContext.flatMap { implicit ec =>
      extractMAuthHeader.flatMap { ahd =>
        extractMwsTimeHeader.flatMap { time =>
          toStrictEntity(timeout) &
            extractRequest.flatMap { req =>
              onComplete(checkSig(req, ahd, time, kl)).flatMap[Unit] {
                case Success(true) => pass
                case _ => reject(MdsolAuthFailedRejection)
              }
            }
        }
      }
    }
  }

  val extractMAuthHeader: Directive1[AuthHeaderDetail] =
    headerValueByType[X_MWS_Authentication]((): Unit).flatMap { hdr =>
      extractAuthHeaderDetail(hdr.value) match {
        case Some(ahd: AuthHeaderDetail) => provide(ahd)
        case None =>
          val msg = s"${X_MWS_Authentication.name} header supplied with bad format: [${hdr.value}]"
          logger.error(msg)
          reject(MalformedHeaderRejection(headerName = X_MWS_Authentication.name, errorMsg = msg, None))

      }
    }

  val extractMwsTimeHeader: Directive1[Long] =
    headerValueByType[X_MWS_Time]((): Unit).flatMap { time =>
      Try(time.value.toLong).toOption match {
        case Some(t: Long) => provide(t)
        case None =>
          val msg = s"${X_MWS_Time.name} header supplied with bad format: [${time.value}]"
          logger.error(msg)
          reject(MalformedHeaderRejection(headerName = X_MWS_Time.name, errorMsg = msg, None))
      }
    }

  /////////////////////////////////////////////
  //  Utility functions
  /////////////////////////////////////////////
  private def checkSig(request: HttpRequest,
                       ahd: AuthHeaderDetail,
                       timestamp: Long,
                       publicKeyProvider: ClientPublicKeyProvider)
                      (implicit ec: ExecutionContext, timeout: FiniteDuration): Future[Boolean] = {
    request.entity match {
      case entity: HttpEntity.Strict => {
        val body = entity.data.utf8String

        publicKeyProvider.getPublicKey(ahd.appId) map {
          case None =>
            logger.warn(s"MAUTH: No public key for App: ${ahd.appId}")
            false

          case Some(k: PublicKey) =>
            val method = HttpVerbOps.httpVerb(request.method)
            val path = request.uri.path.toString()
            val time = timestamp.toString

            val verified = compareDigests(ahd.hash, k, buildSignature(ahd.appId, method, path, body, time))
            logger.debug(s"MAUTH: VERIFIED=$verified")
            verified
        }
      }
      case _ =>
        // should never call this code as this function should always
        // be used after a toStrictEntity Directive
        logger.error(s"MAUTH: Non-Strict Entity in Request")
        FastFuture.successful(false)
    }
  }

  private def extractAuthHeaderDetail(str: String): Option[AuthHeaderDetail] = {
    if (str.startsWith("MWS ")) {
      str.replaceFirst("MWS ", "").split(":").toList match {
        case List(uuid, hash) =>
          try {
            Some(AuthHeaderDetail(UUID.fromString(uuid), hash))
          } catch {
            case NonFatal(e) =>
              logger.error(s"Bad format for UUID in authentication header: $str", e)
              None
          }
        case _ =>
          logger.error(s"Bad format for authentication header: $str")
          None
      }
    } else {
      logger.error(s"Bad format for authentication header: $str")
      None
    }
  }
}

object MAuthDirectives extends MAuthDirectives