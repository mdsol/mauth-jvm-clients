package com.mdsol.mauth.models

import java.net.URI
import java.nio.charset.StandardCharsets

final case class UnsignedRequest(
  httpMethod: String,
  uri: URI,
  body: Array[Byte],
  headers: Map[String, String]
)

object UnsignedRequest {

  /** Create an unsigned request from a string body. The body bytes derived will be the
    * UTF-8 encoded bytes of the body.
    */
  def fromStringBodyUtf8(
    httpMethod: String,
    uri: URI,
    body: String,
    headers: Map[String, String]
  ): UnsignedRequest = {
    apply(
      httpMethod,
      uri,
      body.getBytes(StandardCharsets.UTF_8),
      headers
    )
  }

  def noBody(
    httpMethod: String,
    uri: URI,
    headers: Map[String, String]
  ): UnsignedRequest = {
    apply(
      httpMethod,
      uri,
      body = Array.empty,
      headers = headers
    )
  }
}
