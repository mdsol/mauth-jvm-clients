package com.mdsol.mauth.http4s

import cats.arrow.FunctionK
import cats.data.{Kleisli, OptionT}
import cats.effect.Sync
import cats.syntax.all._
import cats.effect.kernel.Async
import cats.~>
import com.mdsol.mauth.MAuthRequest
import com.mdsol.mauth.scaladsl.Authenticator
import org.http4s.{Http, HttpApp, HttpRoutes, Response, Status}
import org.http4s.EntityDecoder._
import org.typelevel.ci.CIString
import enumeratum._
import org.typelevel.ci._
import org.typelevel.log4cats.slf4j.Slf4jLogger

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.Duration
import scala.util.Try

final case class MdsolAuthMissingHeaderRejection(headerName: String) extends Throwable

sealed trait HeaderVersion extends EnumEntry

object HeaderVersion extends Enum[HeaderVersion] {
  val values = findValues

  case object V1 extends HeaderVersion {
    val authHeaderName = ci"${MAuthRequest.X_MWS_AUTHENTICATION_HEADER_NAME}"
    val timeHeaderName = ci"${MAuthRequest.X_MWS_TIME_HEADER_NAME}"
  }
  case object V2 extends HeaderVersion {
    val authHeaderName = ci"${MAuthRequest.MCC_AUTHENTICATION_HEADER_NAME}"
    val timeHeaderName = ci"${MAuthRequest.MCC_TIME_HEADER_NAME}"
  }
}

object MAuthMiddleware {
  import HeaderVersion._
  def apply[G[_]: Sync, F[_]](requestValidationTimeout: Duration, authenticator: Authenticator, fk: F ~> G)(http: Http[G, F])(implicit
    ec: ExecutionContext,
    F: Async[F]
  ): Http[G, F] =
    Kleisli { request =>
      val logger = Slf4jLogger.getLogger[G]

      def logAndReturnDefaultUnauthorizedReq(errorLogMsg: String) =
        logger.warn(errorLogMsg) *>
          Response[F](status = Status.Unauthorized).pure[G]

      def extractHeader[A](headerName: CIString)(f: String => F[A]) =
        request.headers
          .get(headerName)
          .map(_.head)
          .fold(F.raiseError[A](MdsolAuthMissingHeaderRejection(headerName.toString))) { header =>
            f(header.value)
          }

      def extractAll(headerVersion: HeaderVersion) = {
        val (ahn, thn) = headerVersion match {
          case V1 => (V1.authHeaderName, V1.timeHeaderName)
          case V2 => (V2.authHeaderName, V2.timeHeaderName)
        }
        for {
          authHeadValue <- extractHeader(ahn)(s => s.pure[F])
          timeHeadValue <- extractHeader(thn)(s => Try(s.toLong).liftTo[F])
        } yield (authHeadValue, timeHeadValue)

      }

      def getHeaderValOrEmpty(headerName: CIString) =
        request.headers.get(headerName).map(_.head).fold("")(h => h.value)

      val authHeaderTimeHeader =
        if (authenticator.isV2OnlyAuthenticate)
          extractAll(V2)
        else
          extractAll(V2) orElse extractAll(V1)

      fk(request.as[Array[Byte]].flatMap { byteArray =>
        authHeaderTimeHeader.flatMap { case (authHeader, timeHeader) =>
          val mAuthRequest: MAuthRequest = new MAuthRequest(
            authHeader,
            byteArray,
            request.method.name,
            timeHeader.toString,
            request.uri.path.renderString,
            request.uri.query.renderString
          )

          // this mimics MAuthDirectives in the akka package - really needed?
          val req = if (!authenticator.isV2OnlyAuthenticate) {
            mAuthRequest.setXmwsSignature(getHeaderValOrEmpty(V1.authHeaderName)) // dreadful mutating type
            mAuthRequest.setXmwsTime(getHeaderValOrEmpty(V1.timeHeaderName))
            mAuthRequest
          } else mAuthRequest

          F.fromFuture(F.delay(authenticator.authenticate(req)(ec, requestValidationTimeout)))
        }
      }).flatMap(b =>
        if (b) http(request)
        else logAndReturnDefaultUnauthorizedReq(s"Rejecting request as authentication failed")
      ).recoverWith { case MdsolAuthMissingHeaderRejection(hn) =>
        logAndReturnDefaultUnauthorizedReq(s"Rejecting request as header $hn missing")
      }
    }

  def httpRoutes[F[_]: Async](requestValidationTimeout: Duration, authenticator: Authenticator)(httpRoutes: HttpRoutes[F])(implicit
    ec: ExecutionContext
  ): HttpRoutes[F] = apply(requestValidationTimeout, authenticator, OptionT.liftK[F])(httpRoutes)

  def httpApp[F[_]: Async](requestValidationTimeout: Duration, authenticator: Authenticator)(httpRoutes: HttpApp[F])(implicit
    ec: ExecutionContext
  ): HttpApp[F] = apply(requestValidationTimeout, authenticator, FunctionK.id[F])(httpRoutes)
}
