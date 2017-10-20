package com.mdsol.mauth.akka.http

import java.net.URI
import java.security.PublicKey
import java.util.UUID

import _root_.akka.http.scaladsl.model.StatusCodes
import akka.actor.ActorSystem
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.ActorMaterializer
import com.fasterxml.jackson.databind.ObjectMapper
import com.mdsol.mauth.exception.HttpClientPublicKeyProviderException
import com.mdsol.mauth.http.HttpClient
import com.mdsol.mauth.util.MAuthKeysHelper
import com.mdsol.mauth.utils.async.ClientPublicKeyProviderAsync
import com.mdsol.mauth.{AuthenticatorConfiguration, MAuthRequestSigner, UnsignedRequest}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.language.postfixOps
import scalacache._
import scalacache.guava._
import scalacache.memoization._

class MauthPublicKeyProvider(configuration: AuthenticatorConfiguration, signer: MAuthRequestSigner) extends ClientPublicKeyProviderAsync {

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
  override def getPublicKey(appUUID: UUID): Future[PublicKey] = memoize(60 seconds) {
    val request = UnsignedRequest("GET", new URI(getRequestUrlPath(appUUID)))
    val value = signer.signRequest(request)
    value match {
      case Left(e) => throw e
      case Right(signedRequest) =>
        val eventualResponse = HttpClient.call(signedRequest)
        eventualResponse.flatMap { response =>
          if (response.status == StatusCodes.OK) {
            Unmarshal(response.entity).to[String].map { body =>
              MAuthKeysHelper.getPublicKeyFromString(mapper.readTree(body).findValue("public_key_str").asText)
            }
          }
          else throw new HttpClientPublicKeyProviderException("Invalid response code returned by server: " + response.status)
        }
    }
  }

  private def getRequestUrlPath(appUUID: UUID) = configuration.getRequestUrlPath + String.format(configuration.getSecurityTokensUrlPath, appUUID.toString)
}