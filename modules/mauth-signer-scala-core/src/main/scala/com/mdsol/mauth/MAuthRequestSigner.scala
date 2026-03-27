package com.mdsol.mauth

import com.mdsol.mauth.models.{SignedRequest => NewSignedRequest, UnsignedRequest => NewUnsignedRequest}
import com.mdsol.mauth.util.{CurrentEpochTimeProvider, EpochTimeProvider, MAuthKeysHelper}

import java.security.PrivateKey
import java.util.{List, UUID}
import scala.jdk.CollectionConverters._

case class CryptoError(msg: String, cause: Option[Throwable] = None)

trait RequestSigner {
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
