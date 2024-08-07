package com.mdsol.mauth.http4s

import cats.arrow.FunctionK
import cats.data.{Kleisli, OptionT}
import cats.effect.Sync
import cats.syntax.all._
import cats.effect.kernel.Async
import cats.~>
import com.mdsol.mauth.MAuthRequest
import com.mdsol.mauth.scaladsl.Authenticator
import org.http4s._
import org.http4s.EntityDecoder._
import org.typelevel.ci.CIString
import enumeratum._
import org.typelevel.ci._
import org.typelevel.log4cats.slf4j.Slf4jLogger

import scala.concurrent.duration.Duration
import scala.util.Try

final case class MdsolAuthMissingHeaderRejection(headerName: String) extends Throwable

sealed trait HeaderVersion extends EnumEntry

object HeaderVersion extends Enum[HeaderVersion] {
  val values: IndexedSeq[HeaderVersion] = findValues

  case object V1 extends HeaderVersion {
    val authHeaderName = ci"${MAuthRequest.X_MWS_AUTHENTICATION_HEADER_NAME}"
    val timeHeaderName = ci"${MAuthRequest.X_MWS_TIME_HEADER_NAME}"
  }
  case object V2 extends HeaderVersion {
    val authHeaderName = ci"${MAuthRequest.MCC_AUTHENTICATION_HEADER_NAME}"
    val timeHeaderName = ci"${MAuthRequest.MCC_TIME_HEADER_NAME}"
  }
}

final case class MAuthContext(authHeader: String, timeHeader: Long)

object MAuthMiddleware {
  import HeaderVersion._
  def apply[G[_]: Sync, F[_]](requestValidationTimeout: Duration, authenticator: Authenticator[F], fk: F ~> G)(
    http: Kleisli[G, AuthedRequest[F, MAuthContext], Response[F]]
  )(implicit F: Async[F]): Http[G, F] =
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
        } yield MAuthContext(authHeadValue, timeHeadValue)

      }

      def getHeaderValOrEmpty(headerName: CIString) =
        request.headers.get(headerName).map(_.head).fold("")(h => h.value)

      val authHeaderTimeHeader =
        if (authenticator.isV2OnlyAuthenticate)
          extractAll(V2)
        else
          extractAll(V2) orElse extractAll(V1)

      fk(for {
        strictBody <- request.toStrict(none)
        byteArray <- strictBody.as[Array[Byte]]
        authCtx <- authHeaderTimeHeader
        mAuthRequest = new MAuthRequest(
                         authCtx.authHeader,
                         byteArray,
                         request.method.name,
                         authCtx.timeHeader.toString,
                         request.uri.path.renderString,
                         request.uri.query.renderString
                       )
        req = if (!authenticator.isV2OnlyAuthenticate) {
                mAuthRequest.setXmwsSignature(getHeaderValOrEmpty(V1.authHeaderName)) // dreadful mutating type
                mAuthRequest.setXmwsTime(getHeaderValOrEmpty(V1.timeHeaderName))
                mAuthRequest
              } else mAuthRequest
        res <- authenticator.authenticate(req)(requestValidationTimeout).map(res => (res, authCtx))
      } yield res)
        .flatMap { case (b, ctx) =>
          if (b) http(AuthedRequest(ctx, request))
          else logAndReturnDefaultUnauthorizedReq(s"Rejecting request as authentication failed")
        }
        .recoverWith { case MdsolAuthMissingHeaderRejection(hn) =>
          logAndReturnDefaultUnauthorizedReq(s"Rejecting request as header $hn missing")
        }
    }

  def httpRoutes[F[_]: Async](requestValidationTimeout: Duration, authenticator: Authenticator[F])(
    httpRoutes: HttpRoutes[F]
  ): HttpRoutes[F] = apply(requestValidationTimeout, authenticator, OptionT.liftK[F])(Kleisli(contextRequest => httpRoutes(contextRequest.req)))

  def httpAuthRoutes[F[_]: Async](requestValidationTimeout: Duration, authenticator: Authenticator[F])(
    httpRoutes: AuthedRoutes[MAuthContext, F]
  ): HttpRoutes[F] = apply(requestValidationTimeout, authenticator, OptionT.liftK[F])(httpRoutes)

  def httpApp[F[_]: Async](requestValidationTimeout: Duration, authenticator: Authenticator[F])(
    httpRoutes: Kleisli[F, AuthedRequest[F, MAuthContext], Response[F]]
  ): HttpApp[F] = apply(requestValidationTimeout, authenticator, FunctionK.id[F])(httpRoutes)
}
