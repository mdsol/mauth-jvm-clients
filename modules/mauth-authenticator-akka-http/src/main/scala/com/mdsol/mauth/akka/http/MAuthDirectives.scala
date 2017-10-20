package com.mdsol.mauth.akka.http

import java.security.PublicKey
import java.util.UUID

import akka.http.javadsl.server.AuthorizationFailedRejection
import akka.http.scaladsl.model.{HttpEntity, HttpRequest}
import akka.http.scaladsl.server.{Directive0, Directive1, MalformedHeaderRejection, Rejection}
import akka.http.scaladsl.server.Directives.{headerValueByType, reject}
import akka.http.scaladsl.server.directives.BasicDirectives._
import akka.http.scaladsl.server.directives.FutureDirectives.onComplete
import akka.http.scaladsl.util.FastFuture
import com.mdsol.mauth.http.{HttpVerbOps, X_MWS_Authentication, X_MWS_Time}
import com.typesafe.scalalogging.StrictLogging

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration.FiniteDuration
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
    * @param kl
    * @param timeout
    * @return
    */
  def authenticate(implicit kl: MauthPublicKeyProvider, timeout: FiniteDuration): Directive0 = {
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
    headerValueByType[X_MWS_Authentication]((): Unit).flatMap {
      case hdr =>
        extractAuthHeaderDetail(hdr.value) match {
          case Some(ahd: AuthHeaderDetail) => provide(ahd)
          case None =>
            val msg = s"x-mws-authentication header supplied with bad format: [${hdr.value}]"
            logger.error(msg)
            reject(MalformedHeaderRejection(headerName = X_MWS_Authentication.name, errorMsg = msg, None))

        }
    }

  val extractMwsTimeHeader: Directive1[Long] =
    headerValueByType[X_MWS_Time]((): Unit).flatMap {
      time =>
        Try(time.value.toLong).toOption match {
          case Some(t: Long) => provide(t)
          case None =>
            val msg = s"x-mws-time header supplied with bad format: [${time.value}]"
            logger.error(msg)
            reject(MalformedHeaderRejection(headerName = X_MWS_Time.name, errorMsg = msg, None))
        }
    }

  /////////////////////////////////////////////
  //  Utility functions
  /////////////////////////////////////////////

  /**
    * Check MAuth Signature
    *
    * @param request
    * @param ahd
    * @param timestamp
    * @param publicKeyProvier
    * @param ec
    * @param timeout
    * @return
    */
  private def checkSig(request: HttpRequest,
                       ahd: AuthHeaderDetail,
                       timestamp: Long,
                       publicKeyProvier: MauthPublicKeyProvider)
                      (implicit ec: ExecutionContext, timeout: FiniteDuration): Future[Boolean] = {
    request.entity match {
      case entity: HttpEntity.Strict => {
        val body = entity.data.utf8String

        publicKeyProvier.getPublicKey(ahd.appId) map {
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
        logger.error(s"MAUTH: Non-Strict Entity in Request :(")
        FastFuture.successful(false)
    }
  }
  
  private def extractAuthHeaderDetail(str: String): Option[AuthHeaderDetail] = {
    str.startsWith("MWS ") match {
      case true =>
        str.replaceFirst("MWS ", "").split(":").toList match {
          case List(uuid, hash) =>
            try {
              Some(AuthHeaderDetail(UUID.fromString(uuid), hash))
            } catch {
              case NonFatal(e) =>
                println(s"Bad format for UUID in authentication header: $str")
                None
            }
          case _ =>
            println(s"Bad format for authentication header: $str")
            None
        }
      case _ =>
        println(s"Bad format for authentication header: $str")
        None
    }
  }


  /**
    * Convenience method for server side digest authentication
    * @param base64Header Base 64 value taken directly from the authentication header (minus prefix and UUID)
    * @param signatureString The signature String from the buildSignature method
    * @return Boolean true if there is a match, false if not
    */
  def compareDigests(base64Header: String, key: PublicKey, signatureString: String): Boolean = {
    decryptFromBase64(base64Header, key) match {
      case Left(CryptoError(msg, Some(e))) => logger.debug(msg + " : " + e.getMessage, e); false
      case Left(CryptoError(msg, None)) => logger.debug(msg); false
      case Right(headerDigest: Array[Byte]) =>
        val newDigest = asHex(getDigest(signatureString))
        java.util.Arrays.equals(newDigest.getBytes("UTF-8"), headerDigest)
    }
  }
}

object MAuthDirectives extends MAuthDirectives