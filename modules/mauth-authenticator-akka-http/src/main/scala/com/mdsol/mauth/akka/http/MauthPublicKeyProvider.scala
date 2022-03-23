package com.mdsol.mauth.akka.http

import akka.actor.ActorSystem
import akka.http.scaladsl.model.{HttpResponse, StatusCodes}
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.Materializer
import cats.effect.IO
import cats.effect.unsafe.implicits.global
import com.fasterxml.jackson.databind.ObjectMapper
import com.github.benmanes.caffeine.cache.Caffeine
import com.mdsol.mauth.http.HttpClient
import com.mdsol.mauth.http.Implicits._
import com.mdsol.mauth.models.UnsignedRequest
import com.mdsol.mauth.scaladsl.utils.ClientPublicKeyProvider
import com.mdsol.mauth.util.MAuthKeysHelper
import com.mdsol.mauth.{AuthenticatorConfiguration, MAuthRequestSigner}
import com.typesafe.scalalogging.StrictLogging
import scalacache.caffeine.CaffeineCache
import scalacache.memoization._
import scalacache.{Cache, Entry}

import java.net.URI
import java.security.PublicKey
import java.util.UUID
import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

class MauthPublicKeyProvider(configuration: AuthenticatorConfiguration, signer: MAuthRequestSigner)(implicit
  ec: ExecutionContext,
  system: ActorSystem,
  materializer: Materializer
) extends ClientPublicKeyProvider
    with StrictLogging {

  private val cCache = Caffeine.newBuilder().build[String, Entry[Option[PublicKey]]]()
  implicit val caffeineCache: Cache[IO, String, Option[PublicKey]] = CaffeineCache[IO, String, Option[PublicKey]](underlying = cCache)
  protected val mapper = new ObjectMapper

  /** Returns the associated public key for a given application UUID.
    *
    * @param appUUID , UUID of the application for which we want to retrieve its public key.
    * @return { @link PublicKey} registered in MAuth for the application with given appUUID.
    */
  override def getPublicKey(appUUID: UUID): Future[Option[PublicKey]] =
    getPublicKeyIO(appUUID).unsafeToFuture()

  override def getPublicKeyIO(appUUID: UUID): IO[Option[PublicKey]] = memoizeF(Some(configuration.getTimeToLive.seconds)) {
    val signedRequest = signer.signRequest(UnsignedRequest.noBody("GET", new URI(configuration.getBaseUrl + getRequestUrlPath(appUUID)), headers = Map.empty))
    retrievePublicKey()(IO.fromFuture(IO(HttpClient.call(signedRequest.toAkkaHttpRequest))))
  }

  protected def retrievePublicKey()(mauthPublicKeyFetcher: => IO[HttpResponse]): IO[Option[PublicKey]] = {
    mauthPublicKeyFetcher
      .flatMap { response =>
        IO.fromFuture(
          IO(
            Unmarshal(response.entity)
              .to[String]
          )
        ).map { body =>
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
        }.handleError { error: Throwable =>
          logger.error("Request to get MAuth public key couldn't be signed", error)
          None
        }
      }
      .handleError { error: Throwable =>
        logger.error("Request to get MAuth public key couldn't be completed", error)
        None
      }
  }

  protected def getRequestUrlPath(appUUID: UUID): String =
    configuration.getRequestUrlPath + String.format(configuration.getSecurityTokensUrlPath, appUUID.toString)
}
