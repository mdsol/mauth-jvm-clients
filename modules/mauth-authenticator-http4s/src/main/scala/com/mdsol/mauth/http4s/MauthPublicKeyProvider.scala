package com.mdsol.mauth.http4s

import cats.effect.{Async, Sync}
import com.fasterxml.jackson.databind.ObjectMapper
import com.github.benmanes.caffeine.cache.Caffeine
import com.mdsol.mauth.http4s.client.Implicits.NewSignedRequestOps
import com.mdsol.mauth.models.UnsignedRequest
import com.mdsol.mauth.scaladsl.utils.ClientPublicKeyProvider
import com.mdsol.mauth.util.MAuthKeysHelper
import com.mdsol.mauth.{AuthenticatorConfiguration, MAuthRequestSigner}
import org.http4s.client.Client
import org.http4s.{EntityDecoder, Response, Status}
import scalacache.caffeine.CaffeineCache
import scalacache.memoization.memoizeF
import scalacache.{Cache, Entry}

import java.net.URI
import java.security.PublicKey
import java.util.UUID
import scala.concurrent.duration._
import scala.util.{Failure, Success, Try}
import cats.implicits._
import com.mdsol.mauth.http4s.MauthPublicKeyProvider.SecurityToken
import io.circe.{Decoder, HCursor}
import org.typelevel.log4cats.slf4j.Slf4jLogger
import org.http4s.circe.jsonOf

class MauthPublicKeyProvider[F[_]: Async](configuration: AuthenticatorConfiguration, signer: MAuthRequestSigner, val client: Client[F])
    extends ClientPublicKeyProvider[F] {

  private val logger = Slf4jLogger.getLogger[F]
  private val cCache = Caffeine.newBuilder().build[String, Entry[Option[PublicKey]]]()
  implicit val caffeineCache: Cache[F, String, Option[PublicKey]] = CaffeineCache[F, String, Option[PublicKey]](underlying = cCache)
  protected val mapper = new ObjectMapper

  /** Returns the associated public key for a given application UUID.
    *
    * @param appUUID , UUID of the application for which we want to retrieve its public key.
    * @return { @link PublicKey} registered in MAuth for the application with given appUUID.
    */
  override def getPublicKey(appUUID: UUID): F[Option[PublicKey]] = memoizeF(Some(configuration.getTimeToLive.seconds)) {
    val uri = new URI(configuration.getBaseUrl + getRequestUrlPath(appUUID))
    val signedRequest = signer.signRequest(UnsignedRequest.noBody("GET", uri, headers = Map.empty))
    client.run(signedRequest.toHttp4sRequest[F]).use(retrievePublicKey)
  }

  private def retrievePublicKey(mauthPublicKeyFetcher: Response[F]): F[Option[PublicKey]] = {
    if (mauthPublicKeyFetcher.status == Status.Ok) {
      mauthPublicKeyFetcher
        .attemptAs[SecurityToken]
        .bimap(
          error => {
            logger.error(error)("Converting json to SecurityToken failed")
            None
          },
          securityToken =>
            Try(
              MAuthKeysHelper.getPublicKeyFromString(securityToken.publicKeyStr)
            ) match {
              case Success(publicKey) => Some(publicKey)
              case Failure(error) =>
                logger.error(error)("Converting string to Public Key failed")
                None
            }
        )
        .toOption
        .value
        .map(_.flatten)
    } else {
      logger.error(s"Unexpected response returned by server -- status: ${mauthPublicKeyFetcher.status} response: ${mauthPublicKeyFetcher.body}")
      Async[F].pure[Option[PublicKey]](None)
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
