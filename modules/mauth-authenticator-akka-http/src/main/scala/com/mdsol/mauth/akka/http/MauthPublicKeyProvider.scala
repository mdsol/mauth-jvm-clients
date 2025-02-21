package com.mdsol.mauth.akka.http

import akka.actor.ActorSystem
import akka.http.caching.scaladsl.CachingSettings
import akka.http.caching.LfuCache
import akka.http.scaladsl.model.{HttpResponse, StatusCodes}
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.Materializer
import cats.effect.IO
import com.fasterxml.jackson.databind.ObjectMapper
import com.mdsol.mauth.http.HttpClient
import com.mdsol.mauth.http.Implicits._
import com.mdsol.mauth.models.UnsignedRequest
import com.mdsol.mauth.scaladsl.utils.ClientPublicKeyProvider
import com.mdsol.mauth.util.MAuthKeysHelper
import com.mdsol.mauth.{AuthenticatorConfiguration, MAuthRequestSigner}
import com.typesafe.scalalogging.StrictLogging

import java.net.URI
import java.security.PublicKey
import java.util.UUID
import scala.concurrent.duration.DurationLong
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

class MauthPublicKeyProvider(configuration: AuthenticatorConfiguration, signer: MAuthRequestSigner)(implicit
  ec: ExecutionContext,
  system: ActorSystem,
  materializer: Materializer
) extends ClientPublicKeyProvider[Future]
    with StrictLogging {

  protected val mapper = new ObjectMapper

  private val defaultCachingSettings = CachingSettings(system)
  private val lfuCacheSettings = defaultCachingSettings.lfuCacheSettings.withTimeToLive(configuration.getTimeToLive.seconds)
  private val cache = LfuCache.apply[UUID, Option[PublicKey]](defaultCachingSettings.withLfuCacheSettings(lfuCacheSettings))

  /** Returns the associated public key for a given application UUID.
    *
    * @param appUUID , UUID of the application for which we want to retrieve its public key.
    * @return { @link PublicKey} registered in MAuth for the application with given appUUID.
    */
  override def getPublicKey(appUUID: UUID): Future[Option[PublicKey]] =
    cache.getOrLoad(
      appUUID,
      _ => {
        val signedRequest =
          signer.signRequest(UnsignedRequest.noBody("GET", new URI(configuration.getBaseUrl + getRequestUrlPath(appUUID)), headers = Map.empty))
        retrievePublicKey()(HttpClient.call(signedRequest.toAkkaHttpRequest))

      }
    )

  def getPublicKeyIO(appUUID: UUID): IO[Option[PublicKey]] = IO.fromFuture(IO(getPublicKey(appUUID)))

  protected def retrievePublicKey()(mauthPublicKeyFetcher: => Future[HttpResponse]): Future[Option[PublicKey]] = {
    mauthPublicKeyFetcher
      .flatMap { response =>
        Unmarshal(response.entity)
          .to[String]
          .map { body =>
            if (response.status == StatusCodes.OK) {
              Try(MAuthKeysHelper.getPublicKeyFromString(mapper.readTree(body).findValue("public_key_str").asText)) match {
                case Success(publicKey) => Some(publicKey)
                case Failure(error) =>
                  logger.error("Converting string to Public Key failed", error)
                  None
              }
            } else {
              logger.error(s"Unexpected response returned by server -- status: ${response.status} response: $body")
              None
            }
          }
          .recover[Option[PublicKey]] { case error: Throwable =>
            logger.error("Request to get MAuth public key couldn't be signed", error)
            None
          }
      }
      .recover[Option[PublicKey]] { case error: Throwable =>
        logger.error("Request to get MAuth public key couldn't be completed", error)
        None
      }
  }

  protected def getRequestUrlPath(appUUID: UUID): String =
    configuration.getRequestUrlPath + String.format(configuration.getSecurityTokensUrlPath, appUUID.toString)
}
