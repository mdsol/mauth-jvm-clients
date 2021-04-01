package com.mdsol.mauth

import java.net.URI
import java.security.PrivateKey
import java.util.{List, UUID}

import scala.jdk.CollectionConverters._
import models.{SignedRequest => NewSignedRequest, UnsignedRequest => NewUnsignedRequest}
import com.mdsol.mauth.util.{CurrentEpochTimeProvider, EpochTimeProvider, MAuthKeysHelper}

import scala.util.{Failure, Success, Try}

/** Library agnostic representation of the data required for a request signing
  *
  * @param httpMethod The HTTP verb of this API call
  * @param uri        The URI of the API call , (host name and port not included)
  * @param body       The body of the request in string form
  */
@deprecated("Use com.mdsol.mauth.models.UnsignedRequest", "3.0.0")
case class UnsignedRequest(httpMethod: String = "GET", uri: URI, body: Option[String] = None, headers: Map[String, String] = Map.empty)

/** Library agnostic representation of a signed request, including header data for V1 or V2
  *
  * @note it includes V2 headers only if V2 only is enabled, otherwise it includes the both V1 and V2 headers
  *
  * @param req           The original request that was used to create this object
  * @param authHeader    The Auth header information (Mauth V1 only for binary compatibility)
  * @param timeHeader    The Time header information (Mauth V1 only for binary compatibility)
  */
@deprecated("Use com.mdsol.mauth.models.SignedRequest", "3.0.0")
case class SignedRequest(req: UnsignedRequest, authHeader: String = "", timeHeader: String = "")

case class CryptoError(msg: String, cause: Option[Throwable] = None)

trait RequestSigner {
  @deprecated("This method only signs requests for MAuth V1. Use the other non-deprecated method", "3.0.0")
  def signRequest(request: UnsignedRequest): Either[Throwable, SignedRequest]

  def signRequest(request: NewUnsignedRequest): NewSignedRequest
}

class MAuthRequestSigner(appUUID: UUID, privateKey: PrivateKey, epochTimeProvider: EpochTimeProvider, signVersions: List[MAuthVersion])
    extends DefaultSigner(appUUID, privateKey, epochTimeProvider, signVersions)
    with RequestSigner {

  def this(appUUID: UUID, privateKey: PrivateKey, epochTimeProvider: EpochTimeProvider) =
    this(appUUID, privateKey, epochTimeProvider, SignerConfiguration.DEFAULT_SIGN_VERSION)

  def this(appUUID: UUID, privateKey: PrivateKey) = this(appUUID, privateKey, new CurrentEpochTimeProvider)

  def this(appUUID: UUID, privateKey: String) = this(appUUID, MAuthKeysHelper.getPrivateKeyFromString(privateKey))

  def this(configuration: SignerConfiguration) = this(configuration.getAppUUID, configuration.getPrivateKey)

  def this(appUUID: UUID, privateKey: String, epochTimeProvider: EpochTimeProvider) =
    this(appUUID, MAuthKeysHelper.getPrivateKeyFromString(privateKey), epochTimeProvider)

  def this(appUUID: UUID, privateKey: String, epochTimeProvider: EpochTimeProvider, signVersions: List[MAuthVersion]) =
    this(appUUID, MAuthKeysHelper.getPrivateKeyFromString(privateKey), epochTimeProvider, signVersions)

  /** Sign a request specification and return the desired header signatures
    *
    * @param request The request to sign
    * @return A signed API request or an Error
    */
  @deprecated("This method only signs requests for MAuth V1. Use the other non-deprecated method", "3.0.0")
  override def signRequest(request: UnsignedRequest): Either[Throwable, SignedRequest] = {
    val body = request.body match {
      case Some(entityBody) => entityBody
      case None             => ""
    }

    Try(generateRequestHeaders(request.httpMethod, request.uri.getPath, body)) match {
      case Success(mauthHeaders) =>
        Right(SignedRequest(request, mauthHeaders.get(MAuthRequest.X_MWS_AUTHENTICATION_HEADER_NAME), mauthHeaders.get(MAuthRequest.X_MWS_TIME_HEADER_NAME)))
      case Failure(e) => Left(e)
    }
  }

  override def signRequest(request: NewUnsignedRequest): NewSignedRequest = {
    val javaUri = request.uri
    val headers = SignerUtils.signWithUri(this, request.httpMethod, javaUri, request.body).asScala.toMap
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

  def apply(appUUID: UUID, privateKey: String, epochTimeProvider: EpochTimeProvider, signVersions: List[MAuthVersion]): MAuthRequestSigner =
    new MAuthRequestSigner(appUUID, privateKey, epochTimeProvider, signVersions)

  def apply(appUUID: UUID, privateKey: PrivateKey, epochTimeProvider: EpochTimeProvider, signVersions: List[MAuthVersion]): MAuthRequestSigner =
    new MAuthRequestSigner(appUUID, privateKey, epochTimeProvider, signVersions)
}
