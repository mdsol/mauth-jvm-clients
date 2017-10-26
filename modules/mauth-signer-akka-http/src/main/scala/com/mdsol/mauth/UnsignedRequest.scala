package com.mdsol.mauth

import java.net.URI
import java.security.PrivateKey
import java.util.UUID

import com.mdsol.mauth.util.EpochTimeProvider

import scala.util.{Failure, Success, Try}

/**
  * Library agnostic representation of the data required for a request signing
  *
  * @param httpMethod The HTTP verb of this API call
  * @param uri        The URI of the API call , (host name and port not included)
  * @param body       The body of the request in string form
  */
case class UnsignedRequest(httpMethod: String = "GET", uri: URI, body: Option[String] = None, headers: Map[String, String] = Map.empty)

/**
  * Library agnostic representation of a signed request, including header data
  *
  * @param req        The original request that was used to create this object
  * @param authHeader The Auth header information
  * @param timeHeader The Time header information
  */
case class SignedRequest(req: UnsignedRequest, authHeader: String, timeHeader: String)

/**
  * Generic crypto error container
  * @param msg
  * @param cause
  */
case class CryptoError(msg: String, cause: Option[Throwable] = None)

trait MAuthRequestSigner extends DefaultSigner {

  /**
    * Sign a request specification and return the desired header signatures
    *
    * @param request The request to sign
    * @return A signed API request or an Error
    */
  def signRequest(request: UnsignedRequest): Either[Throwable, SignedRequest] = {
    val body = request.body match {
      case Some(entityBody) => entityBody
      case None => ""
    }

    Try(generateRequestHeaders(request.httpMethod, request.uri.getPath, body)) match {
      case Success(mauthHeaders) => Right(SignedRequest(request, mauthHeaders.get(MAuthRequest.MAUTH_AUTHENTICATION_HEADER_NAME), mauthHeaders.get(MAuthRequest.MAUTH_TIME_HEADER_NAME)))
      case Failure(e) => Left(e)
    }
  }
}

object MAuthRequestSigner {
  def apply(configuration: SignerConfiguration) = new DefaultSigner(configuration) with MAuthRequestSigner

  def apply(appUUID: UUID, privateKey: String) = new DefaultSigner(appUUID, privateKey) with MAuthRequestSigner

  def apply(appUUID: UUID, privateKey: String, epochTimeProvider: EpochTimeProvider) = new DefaultSigner(appUUID, privateKey, epochTimeProvider) with MAuthRequestSigner

  def apply(appUUID: UUID, privateKey: PrivateKey) = new DefaultSigner(appUUID, privateKey) with MAuthRequestSigner

  def apply(appUUID: UUID, privateKey: PrivateKey, epochTimeProvider: EpochTimeProvider) = new DefaultSigner(appUUID, privateKey, epochTimeProvider) with MAuthRequestSigner
}