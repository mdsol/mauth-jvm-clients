package com.mdsol.mauth.http4s

import cats.effect.Async
import com.fasterxml.jackson.databind.ObjectMapper
import com.github.benmanes.caffeine.cache.Caffeine
import com.mdsol.mauth.http4s.client.Implicits.NewSignedRequestOps
import com.mdsol.mauth.models.UnsignedRequest
import com.mdsol.mauth.scaladsl.utils.ClientPublicKeyProvider
import com.mdsol.mauth.util.MAuthKeysHelper
import com.mdsol.mauth.{AuthenticatorConfiguration, MAuthRequestSigner}
import com.typesafe.scalalogging.StrictLogging
import org.http4s.client.Client
import org.http4s.{Response, Status}
import scalacache.caffeine.CaffeineCache
import scalacache.memoization.memoizeF
import scalacache.{Cache, Entry}

import java.net.URI
import java.security.PublicKey
import java.util.UUID
import scala.concurrent.duration._
import scala.util.{Failure, Success, Try}
import cats.implicits._

class MauthPublicKeyProvider[F[_]](configuration: AuthenticatorConfiguration, signer: MAuthRequestSigner, val client: Client[F])
                                  (implicit F: Async[F])
  extends ClientPublicKeyProvider[F]
    with StrictLogging {

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
      mauthPublicKeyFetcher.as[String].map(str =>
        Try(
          MAuthKeysHelper.getPublicKeyFromString(
            mapper.readTree(str).findValue("public_key_str").asText()
          )
        ) match {
          case Success(publicKey) => Some(publicKey)
          case Failure(error) =>
            logger.error("Converting string to Public Key failed", error)
            None
        }
      )
    } else {
      logger.error(s"Unexpected response returned by server -- status: ${mauthPublicKeyFetcher.status} response: ${mauthPublicKeyFetcher.body}")
      F.pure[Option[PublicKey]](None)
    }
  }

  protected def getRequestUrlPath(appUUID: UUID): String =
    configuration.getRequestUrlPath + String.format(configuration.getSecurityTokensUrlPath, appUUID.toString)
}
