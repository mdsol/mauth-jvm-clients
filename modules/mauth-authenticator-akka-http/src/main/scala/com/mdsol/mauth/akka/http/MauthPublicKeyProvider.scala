package com.mdsol.mauth.akka.http

import java.net.URI
import java.security.PublicKey
import java.util.UUID

import akka.actor.ActorSystem
import akka.http.scaladsl.model.{HttpResponse, StatusCodes}
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.Materializer
import com.fasterxml.jackson.databind.ObjectMapper
import com.mdsol.mauth.http.HttpClient
import com.mdsol.mauth.models.UnsignedRequest
import com.mdsol.mauth.scaladsl.utils.ClientPublicKeyProvider
import com.mdsol.mauth.util.MAuthKeysHelper
import com.mdsol.mauth.{AuthenticatorConfiguration, MAuthRequestSigner}
import com.typesafe.scalalogging.StrictLogging
import scalacache.guava._
import scalacache.memoization._
import scalacache.modes.scalaFuture._
import com.mdsol.mauth.http.Implicits._

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.util.{Failure, Success, Try}

class MauthPublicKeyProvider(configuration: AuthenticatorConfiguration, signer: MAuthRequestSigner)(
  implicit ec: ExecutionContext,
  system: ActorSystem,
  materializer: Materializer
) extends ClientPublicKeyProvider
    with StrictLogging {

  implicit val guavaCache: GuavaCache[Option[PublicKey]] = GuavaCache[Option[PublicKey]]
  protected val mapper = new ObjectMapper

  /**
    * Returns the associated public key for a given application UUID.
    *
    * @param appUUID , UUID of the application for which we want to retrieve its public key.
    * @return { @link PublicKey} registered in MAuth for the application with given appUUID.
    */
  override def getPublicKey(appUUID: UUID): Future[Option[PublicKey]] = memoizeF(Some(configuration.getTimeToLive.seconds)) {
    val signedRequest = signer.signRequest(UnsignedRequest.noBody("GET", new URI(configuration.getBaseUrl + getRequestUrlPath(appUUID)), headers = Map.empty))
    retrievePublicKey()(HttpClient.call(signedRequest.toAkkaHttpRequest))
  }

  protected def retrievePublicKey()(mauthPublicKeyFetcher: => Future[HttpResponse]): Future[Option[PublicKey]] = {
    val promise = Promise[Option[PublicKey]]()
    mauthPublicKeyFetcher
      .flatMap { response =>
        Unmarshal(response.entity)
          .to[String]
          .map { body =>
            if (response.status == StatusCodes.OK) {
              Try(MAuthKeysHelper.getPublicKeyFromString(mapper.readTree(body).findValue("public_key_str").asText)) match {
                case Success(publicKey) => promise.success(Some(publicKey))
                case Failure(error) =>
                  logger.error("Converting string to Public Key failed", error)
                  promise.success(None)
              }
            } else {
              logger.error(s"Unexpected response returned by server -- status: ${response.status} response: $body")
              promise.success(None)
            }
          }
          .recover {
            case error =>
              logger.error("Request to get MAuth public key couldn't be signed", error)
              promise.success(None)
          }
      }
      .recover {
        case error =>
          logger.error("Request to get MAuth public key couldn't be completed", error)
          promise.success(None)
      }
    promise.future
  }

  protected def getRequestUrlPath(appUUID: UUID): String =
    configuration.getRequestUrlPath + String.format(configuration.getSecurityTokensUrlPath, appUUID.toString)
}
