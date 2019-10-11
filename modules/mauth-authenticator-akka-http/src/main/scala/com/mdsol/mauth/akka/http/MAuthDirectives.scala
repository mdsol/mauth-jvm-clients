package com.mdsol.mauth.akka.http

import java.util.UUID

import akka.http.javadsl.server.AuthorizationFailedRejection
import akka.http.scaladsl.model.HttpEntity
import akka.http.scaladsl.server.Directives.{headerValueByName, headerValueByType}
import akka.http.scaladsl.server._
import akka.http.scaladsl.server.directives.BasicDirectives._
import akka.http.scaladsl.server.directives.FutureDirectives.onComplete
import akka.http.scaladsl.server.directives.RouteDirectives.reject
import com.mdsol.mauth.MAuthRequest
import com.mdsol.mauth.http.{`X-MWS-Authentication`, `X-MWS-Time`, HttpVerbOps}
import com.mdsol.mauth.scaladsl.Authenticator
import com.typesafe.scalalogging.StrictLogging

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.{Duration, FiniteDuration}
import scala.language.postfixOps
import scala.util.control.NonFatal
import scala.util.{Success, Try}

case class AuthHeaderDetail(appId: UUID, hash: String)

case object MdsolAuthFailedRejection extends AuthorizationFailedRejection with Rejection

trait MAuthDirectives extends StrictLogging {

  /**
    * Directive to wrap all routes that require MAuth authentication check.
    * Should only be used once per route branch, as any HttpEntity is forced
    * to be strict, and serialised into the request.
    *
    * @param authenticator            MAuth Public Key Provider
    * @param timeout                  request timeout duration, defaults to 10 seconds
    * @param requestValidationTimeout request validation timeout duration, defaults to 10 seconds
    * @return Directive to authenticate the request
    */
  def authenticate(implicit ex: ExecutionContext, authenticator: Authenticator, timeout: FiniteDuration, requestValidationTimeout: Duration): Directive0 = {
    extractExecutionContext.flatMap { implicit ec =>
      extractMwsAuthenticationHeader.flatMap { mAuthHeader =>
        extractMwsTimeHeader.flatMap { time =>
          toStrictEntity(timeout) &
            extractRequest.flatMap { req =>
              val isAuthed: Directive[Unit] = req.entity match {
                case entity: HttpEntity.Strict =>
                  onComplete(
                    authenticator.authenticate(
                      new MAuthRequest(mAuthHeader, entity.data.toArray[Byte], HttpVerbOps.httpVerb(req.method), time.toString, req.uri.path.toString)
                    )(ec, requestValidationTimeout)
                  ).flatMap[Unit] {
                    case Success(true) => pass
                    case _ => reject(MdsolAuthFailedRejection)
                  }
                case _ =>
                  logger.error(s"MAUTH: Non-Strict Entity in Request")
                  reject(MdsolAuthFailedRejection)
              }
              isAuthed
            }
        }
      }
    }
  }

  val extractMwsAuthenticationHeader: Directive1[String] = headerValueByName(`X-MWS-Authentication`.name)

  val extractMAuthHeader: Directive1[AuthHeaderDetail] =
    headerValueByType[`X-MWS-Authentication`]((): Unit).flatMap { hdr =>
      extractAuthHeaderDetail(hdr.value) match {
        case Some(ahd: AuthHeaderDetail) => provide(ahd)
        case None =>
          val msg = s"${`X-MWS-Authentication`.name} header supplied with bad format: [${hdr.value}]"
          logger.error(msg)
          reject(MalformedHeaderRejection(headerName = `X-MWS-Authentication`.name, errorMsg = msg, None))

      }
    }

  val extractMwsTimeHeader: Directive1[Long] =
    headerValueByType[`X-MWS-Time`]((): Unit).flatMap { time =>
      Try(time.value.toLong).toOption match {
        case Some(t: Long) => provide(t)
        case None =>
          val msg = s"${`X-MWS-Time`.name} header supplied with bad format: [${time.value}]"
          logger.error(msg)
          reject(MalformedHeaderRejection(headerName = `X-MWS-Time`.name, errorMsg = msg, None))
      }
    }

  /////////////////////////////////////////////
  //  Utility functions
  /////////////////////////////////////////////

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
