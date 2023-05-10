package com.mdsol.mauth.http4s

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import com.fasterxml.jackson.databind.ObjectMapper
import com.github.benmanes.caffeine.cache.Caffeine
import com.mdsol.mauth.http4s.client.Implicits.NewSignedRequestOps
import com.mdsol.mauth.models.UnsignedRequest
import com.mdsol.mauth.{AuthenticatorConfiguration, MAuthRequestSigner}
import com.mdsol.mauth.scaladsl.utils.ClientPublicKeyProvider
import com.mdsol.mauth.util.MAuthKeysHelper
import com.typesafe.scalalogging.StrictLogging
import org.http4s.{Response, Status}
import org.http4s.dsl.Http4sDsl
import scalacache.caffeine.CaffeineCache
import scalacache.memoization.memoizeF
import scalacache.{Cache, Entry}
import org.http4s.client.Client

import java.net.URI
import java.security.PublicKey
import java.util.UUID
import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

class MauthPublicKeyProvider(configuration: AuthenticatorConfiguration, signer: MAuthRequestSigner)(implicit
                                                                                                    ec: ExecutionContext,

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

  def getPublicKeyIO(appUUID: UUID): IO[Option[PublicKey]] = memoizeF(Some(configuration.getTimeToLive.seconds)) {
    val signedRequest = signer.signRequest(UnsignedRequest.noBody("GET", new URI(configuration.getBaseUrl + getRequestUrlPath(appUUID)), headers = Map.empty))
    retrievePublicKey()(Client[IO](signedRequest.toHttp4sRequest => ???))
  }

  protected def retrievePublicKey()(mauthPublicKeyFetcher: => Response[IO]): fs2.Stream[IO, Option[PublicKey]] = {
    if (mauthPublicKeyFetcher.status == Status.Ok) {
      mauthPublicKeyFetcher.body.through(fs2.io.toInputStream).map(is =>
        Try(MAuthKeysHelper.getPublicKeyFromString(
          mapper.readTree(is).findValue("public_key_str").asText())
        ) match {
          case Success(publicKey) => Some(publicKey)
          case Failure(error) =>
            logger.error("Converting string to Public Key failed", error)
            None
        }
      )
    } else {
      logger.error(s"Unexpected response returned by server -- status: ${mauthPublicKeyFetcher.status} response: ${mauthPublicKeyFetcher.body}")
      fs2.Stream[IO, Option[PublicKey]](None)
    }
//
//    mauthPublicKeyFetcher
//      .flatMap { response =>
//        IO.fromFuture(
//          IO(
//            Unmarshal(response.entity)
//              .to[String]
//          )
//        ).map { body =>
//        }.handleError { error: Throwable =>
//          logger.error("Request to get MAuth public key couldn't be signed", error)
//          None
//        }
//      }
//      .handleError { error: Throwable =>
//        logger.error("Request to get MAuth public key couldn't be completed", error)
//        None
//      }
  }

  protected def getRequestUrlPath(appUUID: UUID): String =
    configuration.getRequestUrlPath + String.format(configuration.getSecurityTokensUrlPath, appUUID.toString)
}
