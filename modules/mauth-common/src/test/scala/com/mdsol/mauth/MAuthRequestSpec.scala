package com.mdsol.mauth

import java.nio.charset.StandardCharsets
import java.util.UUID
import java.util.Map

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
  private val CLIENT_REQUEST_AUTHENTICATION_HEADER_V2 = "MWSV2 " + CLIENT_APP_UUID + ":" + CLIENT_REQUEST_SIGNATURE + ";"
  private val CLIENT_REQUEST_TIME_HEADER_V2 = "1444672222"
  private val CLIENT_REQUEST_METHOD = "POST"
  private val CLIENT_REQUEST_PATH = "/resource/path"
  private val CLIENT_REQUEST_PAYLOAD = "message here".getBytes(StandardCharsets.UTF_8)
  private val CLIENT_REQUEST_QUERY_PARAMETERS = "param1=value1&param2=value2";

  private val CLIENT_REQUEST_HEADERS_V1 = new java.util.HashMap[String, String]()
  CLIENT_REQUEST_HEADERS_V1.put(MAuthRequest.X_MWS_AUTHENTICATION_HEADER_NAME, CLIENT_REQUEST_AUTHENTICATION_HEADER)
  CLIENT_REQUEST_HEADERS_V1.put(MAuthRequest.X_MWS_TIME_HEADER_NAME, CLIENT_REQUEST_TIME_HEADER)

  private val CLIENT_REQUEST_HEADERS_V2 = new java.util.HashMap[String, String]()
  CLIENT_REQUEST_HEADERS_V2.put(MAuthRequest.MCC_AUTHENTICATION_HEADER_NAME, CLIENT_REQUEST_AUTHENTICATION_HEADER_V2)
  CLIENT_REQUEST_HEADERS_V2.put(MAuthRequest.MCC_TIME_HEADER_NAME, CLIENT_REQUEST_TIME_HEADER_V2)

  private val CLIENT_REQUEST_HEADERS = new java.util.HashMap[String, String]()
  CLIENT_REQUEST_HEADERS.putAll(CLIENT_REQUEST_HEADERS_V1)
  CLIENT_REQUEST_HEADERS.putAll(CLIENT_REQUEST_HEADERS_V2)

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

  it should "correctly create MAuthRequest for V2" in {
    val request = MAuthRequest.Builder
      .get()
      .withAuthenticationHeaderValue(CLIENT_REQUEST_AUTHENTICATION_HEADER_V2)
      .withTimeHeaderValue(CLIENT_REQUEST_TIME_HEADER_V2)
      .withHttpMethod(CLIENT_REQUEST_METHOD)
      .withMessagePayload(CLIENT_REQUEST_PAYLOAD)
      .withResourcePath(CLIENT_REQUEST_PATH)
      .withQueryParameters(CLIENT_REQUEST_QUERY_PARAMETERS)
      .build()

    request.getAppUUID shouldBe UUID.fromString(CLIENT_APP_UUID)
    request.getHttpMethod shouldBe CLIENT_REQUEST_METHOD
    request.getResourcePath shouldBe CLIENT_REQUEST_PATH
    request.getRequestTime shouldBe CLIENT_REQUEST_TIME_HEADER_V2.toLong
    request.getMessagePayload shouldBe CLIENT_REQUEST_PAYLOAD
    request.getQueryParameters shouldBe CLIENT_REQUEST_QUERY_PARAMETERS
  }

  it should "correctly create MAuthRequest for V2 if V2 only enabled" in {
    val request = MAuthRequest.Builder
      .get()
      .withAuthenticationHeaderValue(CLIENT_REQUEST_AUTHENTICATION_HEADER_V2)
      .withTimeHeaderValue(CLIENT_REQUEST_TIME_HEADER_V2)
      .withHttpMethod(CLIENT_REQUEST_METHOD)
      .withMessagePayload(CLIENT_REQUEST_PAYLOAD)
      .withResourcePath(CLIENT_REQUEST_PATH)
      .withQueryParameters(CLIENT_REQUEST_QUERY_PARAMETERS)
      .build()

    request.getAppUUID shouldBe UUID.fromString(CLIENT_APP_UUID)
    request.getHttpMethod shouldBe CLIENT_REQUEST_METHOD
    request.getResourcePath shouldBe CLIENT_REQUEST_PATH
    request.getRequestTime shouldBe CLIENT_REQUEST_TIME_HEADER_V2.toLong
    request.getMessagePayload shouldBe CLIENT_REQUEST_PAYLOAD
    request.getQueryParameters shouldBe CLIENT_REQUEST_QUERY_PARAMETERS
    request.getMauthVersion shouldBe MAuthVersion.MWSV2
  }

  it should "correctly create MAuthRequest for V1 if the headers include V1 only" in {
    val request = MAuthRequest.Builder
      .get()
      .withHttpMethod(CLIENT_REQUEST_METHOD)
      .withMessagePayload(CLIENT_REQUEST_PAYLOAD)
      .withResourcePath(CLIENT_REQUEST_PATH)
      .withQueryParameters(CLIENT_REQUEST_QUERY_PARAMETERS)
      .withMauthHeaders(CLIENT_REQUEST_HEADERS_V1)
      .build()

    request.getAppUUID shouldBe UUID.fromString(CLIENT_APP_UUID)
    request.getHttpMethod shouldBe CLIENT_REQUEST_METHOD
    request.getResourcePath shouldBe CLIENT_REQUEST_PATH
    request.getRequestTime shouldBe CLIENT_REQUEST_TIME_HEADER.toLong
    request.getMessagePayload shouldBe CLIENT_REQUEST_PAYLOAD
    request.getQueryParameters shouldBe CLIENT_REQUEST_QUERY_PARAMETERS
    request.getMauthVersion shouldBe MAuthVersion.MWS
  }

  it should "correctly create MAuthRequest for V2 if the headers include V2 only" in {
    val request = MAuthRequest.Builder
      .get()
      .withHttpMethod(CLIENT_REQUEST_METHOD)
      .withMessagePayload(CLIENT_REQUEST_PAYLOAD)
      .withResourcePath(CLIENT_REQUEST_PATH)
      .withQueryParameters(CLIENT_REQUEST_QUERY_PARAMETERS)
      .withMauthHeaders(CLIENT_REQUEST_HEADERS_V2)
      .build()

    request.getAppUUID shouldBe UUID.fromString(CLIENT_APP_UUID)
    request.getHttpMethod shouldBe CLIENT_REQUEST_METHOD
    request.getResourcePath shouldBe CLIENT_REQUEST_PATH
    request.getRequestTime shouldBe CLIENT_REQUEST_TIME_HEADER_V2.toLong
    request.getMessagePayload shouldBe CLIENT_REQUEST_PAYLOAD
    request.getQueryParameters shouldBe CLIENT_REQUEST_QUERY_PARAMETERS
    request.getMauthVersion shouldBe MAuthVersion.MWSV2
  }

  it should "correctly create MAuthRequest for V2 if the headers include V1 and V2" in {
    val request = MAuthRequest.Builder
      .get()
      .withHttpMethod(CLIENT_REQUEST_METHOD)
      .withMessagePayload(CLIENT_REQUEST_PAYLOAD)
      .withResourcePath(CLIENT_REQUEST_PATH)
      .withQueryParameters(CLIENT_REQUEST_QUERY_PARAMETERS)
      .withMauthHeaders(CLIENT_REQUEST_HEADERS)
      .build()

    request.getAppUUID shouldBe UUID.fromString(CLIENT_APP_UUID)
    request.getHttpMethod shouldBe CLIENT_REQUEST_METHOD
    request.getResourcePath shouldBe CLIENT_REQUEST_PATH
    request.getRequestTime shouldBe CLIENT_REQUEST_TIME_HEADER_V2.toLong
    request.getMessagePayload shouldBe CLIENT_REQUEST_PAYLOAD
    request.getQueryParameters shouldBe CLIENT_REQUEST_QUERY_PARAMETERS
    request.getMauthVersion shouldBe MAuthVersion.MWSV2
  }

  it should "correctly create MAuthRequest with uppercase headers " in {
    val CLIENT_REQUEST_HEADERS_V2_UPPERCASE = new java.util.HashMap[String, String]()
    CLIENT_REQUEST_HEADERS_V2_UPPERCASE.put(MAuthRequest.MCC_AUTHENTICATION_HEADER_NAME.toUpperCase, CLIENT_REQUEST_AUTHENTICATION_HEADER_V2)
    CLIENT_REQUEST_HEADERS_V2_UPPERCASE.put(MAuthRequest.MCC_TIME_HEADER_NAME.toUpperCase, CLIENT_REQUEST_TIME_HEADER_V2)

    val request = MAuthRequest.Builder
      .get()
      .withHttpMethod(CLIENT_REQUEST_METHOD)
      .withMessagePayload(CLIENT_REQUEST_PAYLOAD)
      .withResourcePath(CLIENT_REQUEST_PATH)
      .withQueryParameters(CLIENT_REQUEST_QUERY_PARAMETERS)
      .withMauthHeaders(CLIENT_REQUEST_HEADERS_V2_UPPERCASE)
      .build()

    request.getAppUUID shouldBe UUID.fromString(CLIENT_APP_UUID)
    request.getHttpMethod shouldBe CLIENT_REQUEST_METHOD
    request.getResourcePath shouldBe CLIENT_REQUEST_PATH
    request.getRequestTime shouldBe CLIENT_REQUEST_TIME_HEADER_V2.toLong
    request.getMessagePayload shouldBe CLIENT_REQUEST_PAYLOAD
    request.getQueryParameters shouldBe CLIENT_REQUEST_QUERY_PARAMETERS
    request.getMauthVersion shouldBe MAuthVersion.MWSV2
  }

  it should "correctly create MAuthRequest (case insensitive) " in {

    val CLIENT_REQUEST_HEADERS_CASE_INSENSITIVE = new java.util.HashMap[String, String]()
    CLIENT_REQUEST_HEADERS_CASE_INSENSITIVE.put("Mcc-Authentication", CLIENT_REQUEST_AUTHENTICATION_HEADER_V2)
    CLIENT_REQUEST_HEADERS_CASE_INSENSITIVE.put("Mcc-Time", CLIENT_REQUEST_TIME_HEADER_V2)

    val request = MAuthRequest.Builder
      .get()
      .withHttpMethod(CLIENT_REQUEST_METHOD)
      .withMessagePayload(CLIENT_REQUEST_PAYLOAD)
      .withResourcePath(CLIENT_REQUEST_PATH)
      .withQueryParameters(CLIENT_REQUEST_QUERY_PARAMETERS)
      .withMauthHeaders(CLIENT_REQUEST_HEADERS_CASE_INSENSITIVE)
      .build()

    request.getAppUUID shouldBe UUID.fromString(CLIENT_APP_UUID)
    request.getHttpMethod shouldBe CLIENT_REQUEST_METHOD
    request.getResourcePath shouldBe CLIENT_REQUEST_PATH
    request.getRequestTime shouldBe CLIENT_REQUEST_TIME_HEADER_V2.toLong
    request.getMessagePayload shouldBe CLIENT_REQUEST_PAYLOAD
    request.getQueryParameters shouldBe CLIENT_REQUEST_QUERY_PARAMETERS
    request.getMauthVersion shouldBe MAuthVersion.MWSV2
  }

}
