package com.mdsol.mauth

import java.net.URI
import java.security.PrivateKey
import java.util.UUID
import scala.collection.JavaConverters._
import models.{UnsignedRequest => NewUnsignedRequest, SignedRequest => NewSignedRequest}

import com.mdsol.mauth.util.{CurrentEpochTimeProvider, EpochTimeProvider, MAuthKeysHelper}

import scala.util.{Failure, Success, Try}
import scala.collection.JavaConverters

/**
  * Library agnostic representation of the data required for a request signing
  *
  * @param httpMethod The HTTP verb of this API call
  * @param uri        The URI of the API call , (host name and port not included)
  * @param body       The body of the request in string form
  */
@deprecated("Use com.mdsol.mauth.models.UnsignedRequest")
case class UnsignedRequest(httpMethod: String = "GET", uri: URI, body: Option[String] = None, headers: Map[String, String] = Map.empty)

/**
  * Library agnostic representation of a signed request, including header data for V1 or V2
  *
  * @note it includes V2 headers only if V2 only is enabled, otherwise it includes the both V1 and V2 headers
  *
  * @param req           The original request that was used to create this object
  * @param authHeader    The Auth header information (Mauth V1 only for binary compatibility)
  * @param timeHeader    The Time header information (Mauth V1 only for binary compatibility)
  */
@deprecated("Use com.mdsol.mauth.models.SignedRequest")
case class SignedRequest(req: UnsignedRequest, authHeader: String = "", timeHeader: String = "")

case class CryptoError(msg: String, cause: Option[Throwable] = None)

trait RequestSigner {
  @deprecated("This method only signs requests for MAuth V1. Use the other non-deprecated method")
  def signRequest(request: UnsignedRequest): Either[Throwable, SignedRequest]

  def signRequest(request: NewUnsignedRequest): NewSignedRequest
}

class MAuthRequestSigner(appUUID: UUID, privateKey: PrivateKey, epochTimeProvider: EpochTimeProvider, v2OnlySignRequests: Boolean)
    extends DefaultSigner(appUUID, privateKey, epochTimeProvider, v2OnlySignRequests)
    with RequestSigner {

  def this(appUUID: UUID, privateKey: PrivateKey, epochTimeProvider: EpochTimeProvider) =
    this(appUUID, privateKey, epochTimeProvider, v2OnlySignRequests = false)

  def this(appUUID: UUID, privateKey: PrivateKey) = this(appUUID, privateKey, new CurrentEpochTimeProvider)

  def this(appUUID: UUID, privateKey: String) = this(appUUID, MAuthKeysHelper.getPrivateKeyFromString(privateKey))

  def this(configuration: SignerConfiguration) = this(configuration.getAppUUID, configuration.getPrivateKey)

  def this(appUUID: UUID, privateKey: String, epochTimeProvider: EpochTimeProvider) =
    this(appUUID, MAuthKeysHelper.getPrivateKeyFromString(privateKey), epochTimeProvider)

  def this(appUUID: UUID, privateKey: String, epochTimeProvider: EpochTimeProvider, v2OnlySignRequests: Boolean) =
    this(appUUID, MAuthKeysHelper.getPrivateKeyFromString(privateKey), epochTimeProvider, v2OnlySignRequests)

  /**
    * Sign a request specification and return the desired header signatures
    *
    * @param request The request to sign
    * @return A signed API request or an Error
    */
  @deprecated("This method only signs requests for MAuth V1. Use the other non-deprecated method")
  override def signRequest(request: UnsignedRequest): Either[Throwable, SignedRequest] = {
    val body = request.body match {
      case Some(entityBody) => entityBody
      case None => ""
    }

    Try(generateRequestHeaders(request.httpMethod, request.uri.getPath, body)) match {
      case Success(mauthHeaders) => Right(
        SignedRequest(request, mauthHeaders.get(MAuthRequest.X_MWS_AUTHENTICATION_HEADER_NAME), mauthHeaders.get(MAuthRequest.X_MWS_TIME_HEADER_NAME)))
      case Failure(e) => Left(e)
    }
  }

  override def signRequest(request: NewUnsignedRequest): NewSignedRequest = {
    val headers = generateRequestHeaders(request.httpMethod, request.uri.getPath, request.body, request.uri.getQuery).asScala.toMap
    NewSignedRequest(
      request,
      headers
    )
  }
}

object MAuthRequestSigner {
  def apply(configuration: SignerConfiguration): MAuthRequestSigner = new MAuthRequestSigner(configuration)

  def apply(appUUID: UUID, privateKey: String): MAuthRequestSigner = new MAuthRequestSigner(appUUID, privateKey)

  def apply(appUUID: UUID, privateKey: String, epochTimeProvider: EpochTimeProvider): MAuthRequestSigner =
    new MAuthRequestSigner(appUUID, privateKey, epochTimeProvider)

  def apply(appUUID: UUID, privateKey: PrivateKey): MAuthRequestSigner = new MAuthRequestSigner(appUUID, privateKey)

  def apply(appUUID: UUID, privateKey: PrivateKey, epochTimeProvider: EpochTimeProvider): MAuthRequestSigner =
    new MAuthRequestSigner(appUUID, privateKey, epochTimeProvider)

  def apply(appUUID: UUID, privateKey: String, epochTimeProvider: EpochTimeProvider, v2OnlySignRequests: Boolean): MAuthRequestSigner =
    new MAuthRequestSigner(appUUID, privateKey, epochTimeProvider, v2OnlySignRequests)

  def apply(appUUID: UUID, privateKey: PrivateKey, epochTimeProvider: EpochTimeProvider, v2OnlySignRequests: Boolean): MAuthRequestSigner =
    new MAuthRequestSigner(appUUID, privateKey, epochTimeProvider, v2OnlySignRequests)
}
