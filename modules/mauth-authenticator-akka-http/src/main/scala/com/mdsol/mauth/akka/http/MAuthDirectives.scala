package com.mdsol.mauth.akka.http

import java.util.UUID
import akka.http.javadsl.model.HttpHeader
import akka.http.javadsl.server.AuthorizationFailedRejection
import akka.http.scaladsl.model.{HttpEntity, HttpRequest}
import akka.http.scaladsl.server.Directives.{headerValueByName, headerValuePF}
import akka.http.scaladsl.server.directives.BasicDirectives._
import akka.http.scaladsl.server.directives.FutureDirectives.onComplete
import akka.http.scaladsl.server.directives.RouteDirectives.reject
import akka.http.scaladsl.server._
import akka.http.scaladsl.server.directives.HeaderMagnet
import com.mdsol.mauth.MAuthRequest
import com.mdsol.mauth.http.{`X-MWS-Authentication`, `X-MWS-Time`, HttpVerbOps}
import com.mdsol.mauth.scaladsl.Authenticator
import com.typesafe.scalalogging.StrictLogging

import scala.concurrent.duration.{Duration, FiniteDuration}
import scala.util.control.NonFatal
import scala.util.{Success, Try}

case class MauthHeaderValues(authenticator: String, time: Long)

case class AuthHeaderDetail(appId: UUID, hash: String)

case object MdsolAuthFailedRejection extends AuthorizationFailedRejection with Rejection

final case class MdsolAuthMalformedHeaderRejection(headerName: String, errorMsg: String, cause: Option[Throwable] = None)
    extends AuthorizationFailedRejection
    with RejectionWithOptionalCause

final case class MdsolAuthMissingHeaderRejection(headerName: String) extends AuthorizationFailedRejection with Rejection

trait MAuthDirectives extends StrictLogging {

  /** Directive to wrap all routes that require MAuth authentication check.
    * Should only be used once per route branch, as any HttpEntity is forced
    * to be strict, and serialised into the request.
    *
    * @param authenticator            MAuth Public Key Provider
    * @param timeout                  request timeout duration, defaults to 10 seconds
    * @param requestValidationTimeout request validation timeout duration, defaults to 10 seconds
    * @return Directive to authenticate the request
    */
  def authenticate(implicit authenticator: Authenticator, timeout: FiniteDuration, requestValidationTimeout: Duration): Directive0 = {
    extractExecutionContext.flatMap { implicit ec =>
      extractLatestAuthenticationHeaders(authenticator.isV2OnlyAuthenticate).flatMap { mauthHeaderValues: MauthHeaderValues =>
        toStrictEntity(timeout) &
          extractRequest.flatMap { req =>
            val isAuthed: Directive[Unit] = req.entity match {
              case entity: HttpEntity.Strict =>
                val mAuthRequest: MAuthRequest = new MAuthRequest(
                  mauthHeaderValues.authenticator,
                  entity.data.toArray[Byte],
                  HttpVerbOps.httpVerb(req.method),
                  mauthHeaderValues.time.toString,
                  req.uri.path.toString,
                  getQueryString(req)
                )
                if (!authenticator.isV2OnlyAuthenticate) {
                  // store V1 headers for fallback to V1 authentication if V2 failed
                  val xmwsAuthenticationHeader = extractRequestHeader(req, MAuthRequest.X_MWS_AUTHENTICATION_HEADER_NAME)
                  val xmwsTimeHeader = extractRequestHeader(req, MAuthRequest.X_MWS_TIME_HEADER_NAME)
                  if (xmwsAuthenticationHeader.nonEmpty && xmwsTimeHeader.nonEmpty) {
                    mAuthRequest.setXmwsSignature(xmwsAuthenticationHeader)
                    mAuthRequest.setXmwsTime(xmwsTimeHeader)
                  }
                }
                onComplete(
                  authenticator.authenticate(
                    mAuthRequest
                  )(ec, requestValidationTimeout)
                ).flatMap[Unit] {
                  case Success(true) => pass
                  case _             => reject(MdsolAuthFailedRejection)
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

  @deprecated("This method is for Mauth V1 protocol only", "3.0.0")
  val extractMwsAuthenticationHeader: Directive1[String] = headerValueByName(`X-MWS-Authentication`.name)

  def headerValueByTypeMdsol[T](magnet: HeaderMagnet[T]): Directive1[T] =
    headerValuePF(magnet.extractPF) | reject(MdsolAuthMissingHeaderRejection(magnet.headerName))

  /** Extracts the detail information of the HTTP request header X-MWS-Authentication
    *
    * @return Directive1[AuthHeaderDetail] of Mauth V1 protocol
    *         If invalidated, the request is rejected with a MalformedHeaderRejection.
    */
  @deprecated("This method is for Mauth V1 protocol only", "3.0.0")
  val extractMAuthHeader: Directive1[AuthHeaderDetail] =
    headerValueByTypeMdsol[`X-MWS-Authentication`]((): Unit).flatMap { hdr =>
      extractAuthHeaderDetail(hdr.value) match {
        case Some(ahd: AuthHeaderDetail) => provide(ahd)
        case None =>
          val msg = s"${`X-MWS-Authentication`.name} header supplied with bad format: [${hdr.value}]"
          logger.error(msg)
          reject(MdsolAuthMalformedHeaderRejection(headerName = `X-MWS-Authentication`.name, errorMsg = msg, None))

      }
    }

  /** Extracts the validated value of the HTTP request header X-MWS-Time
    *
    * @return Directive1[Long] of Mauth V1 protocol
    *         If invalidated, the request is rejected with a MalformedHeaderRejection.
    */
  @deprecated("This method is for Mauth V1 protocol only", "3.0.0")
  val extractMwsTimeHeader: Directive1[Long] =
    headerValueByTypeMdsol[`X-MWS-Time`]((): Unit).flatMap { time =>
      Try(time.value.toLong).toOption match {
        case Some(t: Long) => provide(t)
        case None =>
          val msg = s"${`X-MWS-Time`.name} header supplied with bad format: [${time.value}]"
          logger.error(msg)
          reject(MdsolAuthMalformedHeaderRejection(headerName = `X-MWS-Time`.name, errorMsg = msg, None))
      }
    }

  /////////////////////////////////////////////
  //  Utility functions
  /////////////////////////////////////////////
  private def extractAuthHeaderDetail(str: String): Option[AuthHeaderDetail] = {
    if (str.startsWith("MWS ")) {
      str.replaceFirst("MWS ", "").split(":").toList match {
        case List(uuid, hash) =>
          try Some(AuthHeaderDetail(UUID.fromString(uuid), hash))
          catch {
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

  private def getQueryString(req: HttpRequest): String = req.uri.rawQueryString.getOrElse("")

  private def extractRequestHeader(request: HttpRequest, headerName: String): String = {
    val f = new java.util.function.Function[akka.http.javadsl.model.HttpHeader, String] {
      override def apply(h: HttpHeader): String = h.value()
    }
    request.getHeader(headerName).map[String](f).orElse("")
  }

  /** Extracts the authentication header value of the HTTP request header for latest version of Mauth
    *
    * @param v2OnlyAuthenticate
    *        the flag to specify if Mauth V2 only authenticate or not.
    *        If Mauth v2 only authenticate is enabled, extracts the authentication header of MCC-Authentication only.
    *        Otherwise, extracts the authentication header of X-MWS-Authentication if MCC-Authentication header is not found.
    *
    * @return Directive1[MauthHeaderValues] of Mauth authentication header values for V1 or V2
    *         the request is rejected with a MdsolAuthMissingHeaderRejection if the expected header is not present
    */
  def extractLatestAuthenticationHeaders(v2OnlyAuthenticate: Boolean): Directive1[MauthHeaderValues] = {
    extractRequest.flatMap { request: HttpRequest =>
      // Try to extract and verify V2 headers
      val authenticationHeaderStr = extractRequestHeader(request, MAuthRequest.MCC_AUTHENTICATION_HEADER_NAME)
      if (authenticationHeaderStr.nonEmpty) {
        val timeHeaderStr = extractRequestHeader(request, MAuthRequest.MCC_TIME_HEADER_NAME)
        if (timeHeaderStr.nonEmpty) {
          Try(timeHeaderStr.toLong).toOption match {
            case Some(time: Long) =>
              provide(MauthHeaderValues(authenticationHeaderStr, time))
            case None =>
              val msg = s"${MAuthRequest.MCC_TIME_HEADER_NAME} header supplied with bad format: [$timeHeaderStr]"
              logger.error(msg)
              reject(MdsolAuthMalformedHeaderRejection(headerName = MAuthRequest.MCC_TIME_HEADER_NAME, errorMsg = msg, None))
          }
        } else {
          reject(MdsolAuthMissingHeaderRejection(MAuthRequest.MCC_TIME_HEADER_NAME))
        }
      } else {
        // If V2 headers not found, fallback to V1 headers if allowed
        if (!v2OnlyAuthenticate) {
          val authenticationHeaderStr = extractRequestHeader(request, MAuthRequest.X_MWS_AUTHENTICATION_HEADER_NAME)
          if (authenticationHeaderStr.nonEmpty) {
            val timeHeaderStr = extractRequestHeader(request, MAuthRequest.X_MWS_TIME_HEADER_NAME)
            if (timeHeaderStr.nonEmpty) {
              Try(timeHeaderStr.toLong).toOption match {
                case Some(time: Long) =>
                  provide(MauthHeaderValues(authenticationHeaderStr, time))
                case None =>
                  val msg = s"${MAuthRequest.X_MWS_TIME_HEADER_NAME} header supplied with bad format: [$timeHeaderStr]"
                  logger.error(msg)
                  reject(MdsolAuthMalformedHeaderRejection(headerName = MAuthRequest.X_MWS_TIME_HEADER_NAME, errorMsg = msg, None))
              }
            } else {
              reject(MdsolAuthMissingHeaderRejection(MAuthRequest.X_MWS_TIME_HEADER_NAME))
            }
          } else {
            reject(MdsolAuthMissingHeaderRejection(MAuthRequest.X_MWS_AUTHENTICATION_HEADER_NAME))
          }
        } else {
          reject(MdsolAuthMissingHeaderRejection(MAuthRequest.MCC_AUTHENTICATION_HEADER_NAME))
        }
      }
    }
  }

}

object MAuthDirectives extends MAuthDirectives
