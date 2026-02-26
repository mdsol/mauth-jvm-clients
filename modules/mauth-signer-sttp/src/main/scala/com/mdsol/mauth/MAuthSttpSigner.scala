package com.mdsol.mauth

import java.nio.charset.StandardCharsets
import java.security.PrivateKey
import java.util.UUID

import com.mdsol.mauth.util.EpochTimeProvider

import scala.jdk.CollectionConverters._
import sttp.client4.{BasicMultipartBody, ByteArrayBody, ByteBufferBody, FileBody, InputStreamBody, NoBody, Request, StringBody}
import sttp.model.Header

trait MAuthSttpSigner {
  def signSttpRequest[T](request: Request[T]): Request[T]
}

/** Sign an sttp request by adding MAuth headers to the request */
class MAuthSttpSignerImpl(signer: Signer) extends MAuthSttpSigner {

  def this(appUuid: UUID, privateKey: PrivateKey, epochTimeProvider: EpochTimeProvider, signVersions: java.util.List[MAuthVersion]) = {
    this(new DefaultSigner(appUuid, privateKey, epochTimeProvider, signVersions))
  }

  def signSttpRequest[T](request: Request[T]): Request[T] = {
    val bodyBytes: Array[Byte] = request.body match {
      case NoBody                        => Array.empty[Byte]
      case strBody: StringBody           => strBody.s.getBytes(StandardCharsets.UTF_8)
      case ByteArrayBody(bytes, _)       => bytes
      case ByteBufferBody(byteBuffer, _) => byteBuffer.array()
      // $COVERAGE-OFF$
      case _: InputStreamBody =>
        throw new IllegalArgumentException("Request with InputStream body not supported for mauth signing")
      case _: FileBody =>
        throw new IllegalArgumentException("MAuth signing not yet implemented for request with file body")
      case _: BasicMultipartBody =>
        throw new IllegalArgumentException("MAuth signing not yet implemented for request with multipart body")
      // $COVERAGE-ON$
    }

    val requestUri = request.uri.toJavaUri
    val mauthHeaders: List[Header] = SignerUtils
      .signWithUri(
        signer,
        request.method.method,
        requestUri,
        bodyBytes
      )
      .asScala
      .map { case (k, v) =>
        Header(k, v)
      }
      .toList
    request.headers(mauthHeaders: _*)
  }
}
