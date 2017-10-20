package com.mdsol.mauth.akka.http

import java.net.URI
import java.security.PublicKey
import java.util.UUID

import _root_.akka.http.scaladsl.model.StatusCodes
import akka.actor.ActorSystem
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.ActorMaterializer
import com.fasterxml.jackson.databind.ObjectMapper
import com.mdsol.mauth.http.HttpClient
import com.mdsol.mauth.util.MAuthKeysHelper
import com.mdsol.mauth.utils.async.ClientPublicKeyProviderAsync
import com.mdsol.mauth.{AuthenticatorConfiguration, MAuthRequestSigner, UnsignedRequest}
import com.typesafe.scalalogging.StrictLogging

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Future, Promise}
import scala.language.postfixOps
import scala.util.{Failure, Success, Try}
import scalacache._
import scalacache.guava._
import scalacache.memoization._

class MauthPublicKeyProvider(configuration: AuthenticatorConfiguration, signer: MAuthRequestSigner) extends ClientPublicKeyProviderAsync with StrictLogging {

  implicit val scalaCache: ScalaCache[NoSerialization] = ScalaCache(GuavaCache())
  private val mapper = new ObjectMapper
  implicit val system: ActorSystem = ActorSystem()
  implicit val materializer: ActorMaterializer = ActorMaterializer()

  /**
    * Returns the associated public key for a given application UUID.
    *
    * @param appUUID , UUID of the application for which we want to retrieve its public key.
    * @return { @link PublicKey} registered in MAuth for the application with given appUUID.
    */
  override def getPublicKey(appUUID: UUID): Future[Option[PublicKey]] = memoize(60 seconds) {
    val promise = Promise[Option[PublicKey]]()
    signer.signRequest(UnsignedRequest("GET", new URI(getRequestUrlPath(appUUID)))) match {
      case Left(e) =>
        logger.error("Request to get MAuth public key couldn't be signed", e)
        promise.success(None)
      case Right(signedRequest) =>
        HttpClient.call(signedRequest).flatMap { response =>
          Unmarshal(response.entity).to[String].map { body =>
            if (response.status == StatusCodes.OK) {
              Try(MAuthKeysHelper.getPublicKeyFromString(mapper.readTree(body).findValue("public_key_str").asText)) match {
                case Success(publicKey) => promise.success(Some(publicKey))
                case Failure(error) =>
                  logger.error("Converting string to Public Key failed", error)
                  promise.success(None)
              }
            } else {
              logger.error(s"Unexpected response returned by server -- status: ${response.status} response: $body")
            }
          }.recover {
            case error =>
              logger.error("Request to get MAuth public key couldn't be signed", error)
              promise.success(None)
          }
        }
    }
    promise.future
  }

  private def getRequestUrlPath(appUUID: UUID) = configuration.getRequestUrlPath + String.format(configuration.getSecurityTokensUrlPath, appUUID.toString)
}