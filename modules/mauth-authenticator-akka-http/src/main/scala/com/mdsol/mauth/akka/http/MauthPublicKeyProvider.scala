package com.mdsol.mauth.akka.http

import java.net.URI
import java.security.PublicKey
import java.util.UUID

import _root_.akka.http.scaladsl.model.{HttpResponse, StatusCodes}
import akka.actor.ActorSystem
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.ActorMaterializer
import brave.Span
import com.fasterxml.jackson.databind.ObjectMapper
import com.mdsol.mauth.http.{HttpClient, TraceHttpClient}
import com.mdsol.mauth.scaladsl.utils.ClientPublicKeyProvider
import com.mdsol.mauth.util.MAuthKeysHelper
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

class MauthPublicKeyProvider(configuration: AuthenticatorConfiguration, signer: MAuthRequestSigner)(implicit system: ActorSystem, materializer: ActorMaterializer)
  extends ClientPublicKeyProvider with StrictLogging {

  implicit val scalaCache: ScalaCache[NoSerialization] = ScalaCache(GuavaCache())
  protected val mapper = new ObjectMapper

  /**
    * Returns the associated public key for a given application UUID.
    *
    * @param appUUID , UUID of the application for which we want to retrieve its public key.
    * @return { @link PublicKey} registered in MAuth for the application with given appUUID.
    */
  override def getPublicKey(appUUID: UUID): Future[Option[PublicKey]] = memoize(configuration.getTimeToLive seconds) {
    signer.signRequest(UnsignedRequest("GET", new URI(configuration.getBaseUrl + getRequestUrlPath(appUUID)))) match {
      case Left(e) =>
        logger.error("Request to get MAuth public key couldn't be signed", e)
        Future(None)
      case Right(signedRequest) => retrievePublicKey()(HttpClient.call(signedRequest))
    }
  }

  protected def retrievePublicKey()(mauthPublicKeyFetcher: => Future[HttpResponse]): Future[Option[PublicKey]] = {
    val promise = Promise[Option[PublicKey]]()
    mauthPublicKeyFetcher.flatMap { response =>
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
          promise.success(None)
        }
      }.recover {
        case error =>
          logger.error("Request to get MAuth public key couldn't be signed", error)
          promise.success(None)
      }
    }.recover {
      case error => logger.error("Request to get MAuth public key couldn't be completed", error)
        promise.success(None)
    }
    promise.future
  }

  protected def getRequestUrlPath(appUUID: UUID): String = configuration.getRequestUrlPath + String.format(configuration.getSecurityTokensUrlPath, appUUID.toString)
}

class TraceMauthPublicKeyProvider(configuration: AuthenticatorConfiguration, signer: MAuthRequestSigner, traceHttpClient: TraceHttpClient)(implicit system: ActorSystem, materializer: ActorMaterializer)
  extends MauthPublicKeyProvider(configuration, signer) {

  /**
    * Returns the associated public key for a given application UUID.
    *
    * @param appUUID , UUID of the application for which we want to retrieve its public key.
    * @return { @link PublicKey} registered in MAuth for the application with given appUUID.
    */
  def traceGetPublicKey(appUUID: UUID, traceName: String, parentSpan: Span): Future[Option[PublicKey]] = memoize(configuration.getTimeToLive seconds) {
    signer.signRequest(UnsignedRequest("GET", new URI(configuration.getBaseUrl + getRequestUrlPath(appUUID)))) match {
      case Left(e) =>
        logger.error("Request to get MAuth public key couldn't be signed", e)
        Future(None)
      case Right(signedRequest) => retrievePublicKey()(traceHttpClient.traceCall(signedRequest, traceName, parentSpan))
    }
  }
}
