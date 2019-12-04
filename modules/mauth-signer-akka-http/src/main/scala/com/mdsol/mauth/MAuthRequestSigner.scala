package com.mdsol.mauth

import java.net.URI
import java.security.PrivateKey
import java.util.UUID

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
case class UnsignedRequest(httpMethod: String = "GET", uri: URI, body: Option[String] = None, headers: Map[String, String] = Map.empty)

/**
  * Library agnostic representation of a signed request, including header data for V1 or V2
  *
  * @note it includes V2 headers only if V2 only is enabled, otherwise it includes the both V1 and V2 headers
  *
  * @param req           The original request that was used to create this object
  * @param authHeader    The Auth header information (Mauth V1 only for binary compatibility)
  * @param timeHeader    The Time header information (Mauth V1 only for binary compatibility)
  * @param mauthHeaders  The map of mauth headers ( headers of Mauth V1 and V2)
  */
case class SignedRequest(req: UnsignedRequest, authHeader: String = "", timeHeader: String = "", mauthHeaders: Map[String, String] = Map.empty)

case class CryptoError(msg: String, cause: Option[Throwable] = None)

trait RequestSigner {
  def signRequest(request: UnsignedRequest): Either[Throwable, SignedRequest]
}

class MAuthRequestSigner(appUUID: UUID, privateKey: PrivateKey, epochTimeProvider: EpochTimeProvider, v2OnlySignRequests: Boolean)
    extends DefaultSigner(appUUID, privateKey, epochTimeProvider, v2OnlySignRequests)
    with RequestSigner {

  def this(appUUID: UUID, privateKey: PrivateKey, epochTimeProvider: EpochTimeProvider) =
    this(appUUID, privateKey, epochTimeProvider, false)

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
  override def signRequest(request: UnsignedRequest): Either[Throwable, SignedRequest] = {
    val body = request.body match {
      case Some(entityBody) => entityBody.getBytes
      case None => "".getBytes
    }

    Try(generateRequestHeaders(request.httpMethod, request.uri.getPath, body, request.uri.getQuery)) match {
      case Success(mauthHeaders) =>
        val sMap = JavaConverters.mapAsScalaMapConverter(mauthHeaders).asScala.toMap(Predef.$conforms)
        Right(SignedRequest(request, mauthHeaders = sMap))
      case Failure(e) => Left(e)
    }
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
