package com.mdsol.mauth.http4s

import cats.ApplicativeThrow
import cats.effect.Async
import com.mdsol.mauth.http4s.client.Implicits.NewSignedRequestOps
import com.mdsol.mauth.models.UnsignedRequest
import com.mdsol.mauth.scaladsl.utils.ClientPublicKeyProvider
import com.mdsol.mauth.util.MAuthKeysHelper
import com.mdsol.mauth.{AuthenticatorConfiguration, MAuthRequestSigner}
import org.http4s.client.Client
import org.http4s.{EntityDecoder, Response, Status}
import scalacache.memoization.memoizeF
import scalacache.Cache

import java.net.URI
import java.security.PublicKey
import java.util.UUID
import scala.concurrent.duration._
import cats.implicits._
import com.mdsol.mauth.http4s.MauthPublicKeyProvider.SecurityToken
import io.circe.{Decoder, HCursor}
import org.http4s.circe.jsonOf
import org.typelevel.log4cats.Logger

class MauthPublicKeyProvider[F[_]: Async: Logger](configuration: AuthenticatorConfiguration, signer: MAuthRequestSigner, val client: Client[F])(implicit
  val cache: Cache[F, String, Option[PublicKey]]
) extends ClientPublicKeyProvider[F] {

  /** Returns the associated public key for a given application UUID.
    *
    * @param appUUID , UUID of the application for which we want to retrieve its public key.
    * @return { @link PublicKey} registered in MAuth for the application with given appUUID.
    */
  override def getPublicKey(appUUID: UUID): F[Option[PublicKey]] = memoizeF(Some(configuration.getTimeToLive.seconds)) {
    val uri = new URI(configuration.getBaseUrl + getRequestUrlPath(appUUID))
    val signedRequest = signer.signRequest(UnsignedRequest.noBody("GET", uri, headers = Map.empty))
    signedRequest
      .toHttp4sRequest[F]
      .flatMap(req => client.run(req).use(retrievePublicKey))
  }
  private def retrievePublicKey(mauthPublicKeyFetcher: Response[F]): F[Option[PublicKey]] = {
    mauthPublicKeyFetcher.status match {
      case Status.Ok =>
        mauthPublicKeyFetcher
          .as[SecurityToken]
          .flatMap { securityToken =>
            ApplicativeThrow[F]
              .catchNonFatal(MAuthKeysHelper.getPublicKeyFromString(securityToken.publicKeyStr))
              .map(_.some)
              .recoverWith { case error =>
                Logger[F].error(error)("Converting string to Public Key failed") *> none[PublicKey].pure[F]
              }
          }
          .recoverWith { case error =>
            Logger[F].error(error)("Converting json to SecurityToken failed") *> none[PublicKey].pure[F]
          }
      case _ =>
        Logger[F]
          .error(s"Unexpected response returned by server -- status: ${mauthPublicKeyFetcher.status} response: ${mauthPublicKeyFetcher.body}") *>
          none[PublicKey].pure[F]
    }
  }

  private def getRequestUrlPath(appUUID: UUID): String =
    configuration.getRequestUrlPath + String.format(configuration.getSecurityTokensUrlPath, appUUID.toString)
}

object MauthPublicKeyProvider {

  implicit val securityTokenDecoderInstance: Decoder[SecurityToken] = (c: HCursor) => {
    for {
      appName <- c.downField("security_token").downField("app_name").as[String]
      appUuid <- c.downField("security_token").downField("app_uuid").as[UUID]
      publicKeyStr <- c.downField("security_token").downField("public_key_str").as[String]
    } yield SecurityToken(appName, appUuid, publicKeyStr)
  }
  implicit def entityDecoder[F[_]: Async]: EntityDecoder[F, SecurityToken] = jsonOf[F, SecurityToken]
  final case class SecurityToken(appName: String, appUuid: UUID, publicKeyStr: String)

}
