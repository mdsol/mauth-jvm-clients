package com.mdsol.mauth

import java.nio.charset.StandardCharsets
import java.util.UUID

import org.scalatest.{FlatSpec, Matchers}

class MAuthRequestSpec extends FlatSpec with Matchers {

  private val CLIENT_APP_UUID = "92a1869e-c80d-4f06-8775-6c4ebb0758e0"
  private val CLIENT_REQUEST_SIGNATURE =
    """fFQzIOo4S1MxxmEDB9v7v0IYNytnS3I5aHNeJfEfFe1v1gTE
      |/cH36BfLG/zpOHX7kPUDUnmVZK1MuqdenLDmY6q2h8BR/1yA
      |7n/dupQfnVZIeaB99GIOFJaHT6P6gXHiMTFxgX3Oo/rj97jf
      |DUxaunxnlqnfhHccPxbhiqfcVgHahw6hiXx9sAt/iG/Yu7lz
      |ZbWx4SWkDyAGvk1I5rpbxcOWLPYad8qpuCFaHPMQJTTze8G0
      |2hd5ocZDzQS73MOfUqPLfvv7aLRvBS078cqs2uAip84n8bM4
      |qG60gMfoC9kUluza7i9poyFqqIdsCnS5RQuyNcsixneX2X3CNt3yOw==""".stripMargin
  private val CLIENT_REQUEST_AUTHENTICATION_HEADER = "MWS " + CLIENT_APP_UUID + ":" + CLIENT_REQUEST_SIGNATURE
  private val CLIENT_REQUEST_TIME_HEADER = "1444672122"
  private val CLIENT_REQUEST_METHOD = "POST"
  private val CLIENT_REQUEST_PATH = "/resource/path"
  private val CLIENT_REQUEST_PAYLOAD = "message here".getBytes(StandardCharsets.UTF_8)

  behavior of "MAuthRequest"

  it should "correctly create MAuthRequest" in {
    val request = MAuthRequest.Builder
      .get()
      .withAuthenticationHeaderValue(CLIENT_REQUEST_AUTHENTICATION_HEADER)
      .withTimeHeaderValue(CLIENT_REQUEST_TIME_HEADER)
      .withHttpMethod(CLIENT_REQUEST_METHOD)
      .withMessagePayload(CLIENT_REQUEST_PAYLOAD)
      .withResourcePath(CLIENT_REQUEST_PATH)
      .build()

    request.getAppUUID shouldBe UUID.fromString(CLIENT_APP_UUID)
    request.getHttpMethod shouldBe CLIENT_REQUEST_METHOD
    request.getResourcePath shouldBe CLIENT_REQUEST_PATH
    request.getRequestTime shouldBe CLIENT_REQUEST_TIME_HEADER.toLong
    request.getMessagePayload shouldBe CLIENT_REQUEST_PAYLOAD
  }

  it should "correctly create MAuthRequest without message payload" in {
    val request = MAuthRequest.Builder
      .get()
      .withAuthenticationHeaderValue(CLIENT_REQUEST_AUTHENTICATION_HEADER)
      .withTimeHeaderValue(CLIENT_REQUEST_TIME_HEADER)
      .withHttpMethod(CLIENT_REQUEST_METHOD)
      .withResourcePath(CLIENT_REQUEST_PATH)
      .build()

    request.getAppUUID shouldBe UUID.fromString(CLIENT_APP_UUID)
    request.getHttpMethod shouldBe CLIENT_REQUEST_METHOD
    request.getResourcePath shouldBe CLIENT_REQUEST_PATH
    request.getRequestTime shouldBe CLIENT_REQUEST_TIME_HEADER.toLong
    request.getMessagePayload shouldBe Array[Byte]()
  }

  it should "not allow to create request without request authentication header" in {
    val expectedException = intercept[IllegalArgumentException] {
      MAuthRequest.Builder
        .get()
        .withHttpMethod(CLIENT_REQUEST_METHOD)
        .withMessagePayload(CLIENT_REQUEST_PAYLOAD)
        .withTimeHeaderValue(CLIENT_REQUEST_TIME_HEADER)
        .withResourcePath(CLIENT_REQUEST_PATH)
        .build()
    }
    expectedException.getMessage shouldBe "Authentication header value cannot be null or empty."
  }

  it should "not allow to create request without http method" in {
    val expectedException = intercept[IllegalArgumentException] {
      MAuthRequest.Builder
        .get()
        .withAuthenticationHeaderValue(CLIENT_REQUEST_AUTHENTICATION_HEADER)
        .withTimeHeaderValue(CLIENT_REQUEST_TIME_HEADER)
        .withMessagePayload(CLIENT_REQUEST_PAYLOAD)
        .withResourcePath(CLIENT_REQUEST_PATH)
        .build()
    }
    expectedException.getMessage shouldBe "Http method cannot be null or empty."
  }

  it should "not allow to create request without request time header" in {
    val expectedException = intercept[IllegalArgumentException] {
      MAuthRequest.Builder
        .get()
        .withHttpMethod(CLIENT_REQUEST_METHOD)
        .withAuthenticationHeaderValue(CLIENT_REQUEST_AUTHENTICATION_HEADER)
        .withMessagePayload(CLIENT_REQUEST_PAYLOAD)
        .withResourcePath(CLIENT_REQUEST_PATH)
        .build()
    }
    expectedException.getMessage shouldBe "Time header value cannot be null or empty."
  }

  it should "not allow to create request with negative request time header" in {
    val expectedException = intercept[IllegalArgumentException] {
      MAuthRequest.Builder
        .get()
        .withHttpMethod(CLIENT_REQUEST_METHOD)
        .withAuthenticationHeaderValue(CLIENT_REQUEST_AUTHENTICATION_HEADER)
        .withTimeHeaderValue("-1")
        .withMessagePayload(CLIENT_REQUEST_PAYLOAD)
        .withResourcePath(CLIENT_REQUEST_PATH)
        .build()
    }
    expectedException.getMessage shouldBe "Request time cannot be negative or 0."
  }

  it should "not allow to create request without request path" in {
    val expectedException = intercept[IllegalArgumentException] {
      MAuthRequest.Builder
        .get()
        .withHttpMethod(CLIENT_REQUEST_METHOD)
        .withAuthenticationHeaderValue(CLIENT_REQUEST_AUTHENTICATION_HEADER)
        .withTimeHeaderValue(CLIENT_REQUEST_TIME_HEADER)
        .withMessagePayload(CLIENT_REQUEST_PAYLOAD)
        .build()
    }
    expectedException.getMessage shouldBe "Resource path cannot be null or empty."
  }

}
