package com.mdsol.mauth

import java.nio.charset.StandardCharsets

import com.mdsol.mauth.test.utils.FakeMAuthServer.EXISTING_CLIENT_APP_UUID
import com.mdsol.mauth.test.utils.TestFixtures
import org.apache.hc.client5.http.classic.methods.{HttpGet, HttpPost}

trait RequestAuthenticatorBaseSpec {

  val CLIENT_X_MWS_TIME_HEADER_VALUE = "1444672122"
  val CLIENT_UNICODE_X_MWS_TIME_HEADER_VALUE = "1444748974"
  val CLIENT_NO_BODY_X_MWS_TIME_HEADER_VALUE = "1424700000"
  val PUBLIC_KEY: String = TestFixtures.PUBLIC_KEY_1
  val REQUEST_VALIDATION_TIMEOUT_SECONDS = 300L
  val CLIENT_REQUEST_SIGNATURE: String =
    """fFQzIOo4S1MxxmEDB9v7v0IYNytnS3I5aHNeJfEfFe1v1gTE/cH36BfLG/zp
      |OHX7kPUDUnmVZK1MuqdenLDmY6q2h8BR/1yA7n/dupQfnVZIeaB99GIOFJaH
      |T6P6gXHiMTFxgX3Oo/rj97jfDUxaunxnlqnfhHccPxbhiqfcVgHahw6hiXx9
      |sAt/iG/Yu7lzZbWx4SWkDyAGvk1I5rpbxcOWLPYad8qpuCFaHPMQJTTze8G0
      |2hd5ocZDzQS73MOfUqPLfvv7aLRvBS078cqs2uAip84n8bM4qG60gMfoC9kU
      |luza7i9poyFqqIdsCnS5RQuyNcsixneX2X3CNt3yOw==""".stripMargin
  val CLIENT_REQUEST_AUTHENTICATION_HEADER: String = "MWS " + EXISTING_CLIENT_APP_UUID.toString + ":" + CLIENT_REQUEST_SIGNATURE
  val CLIENT_REQUEST_METHOD: String = HttpPost.METHOD_NAME
  val CLIENT_REQUEST_PATH = "/resource/path"
  val CLIENT_REQUEST_BODY = "message here"
  val CLIENT_UNICODE_REQUEST_SIGNATURE: String =
    """uJ2HR9ntb9v+5cbx8X5y/SYuQCTGz+f9jxk2AJCR02mrW/nWspN1H9jvC6Y
      |AF0W4TJMCWFAXV3rHG5OPnc2aEGGvSddmfW/Bkhx09IA2dRRTQ9JHdSxrQH
      |9BGGkAP0gMUPdP1/WiJvRh9jcEiAUJ4gmpYzL78PCJVI2uAkZm3czmiqXvX
      |YhQmRD0KjurRfecEwAk3VNvSbdWgoO5BM4PTTZULhjU4clfCti6+0X93ffZ
      |QGxkjcSEtIeaz2tci/YUtsYDfbfVeqX2M3//w0OCpcBlHYXuGh9S8I1D2DC
      |cjvC08GMJPj8HIOte0nnsIcFr5SRdfxH+5xgW7OCdUfSsKw==""".stripMargin
  val CLIENT_UNICODE_REQUEST_AUTHENTICATION_HEADER: String = "MWS " + EXISTING_CLIENT_APP_UUID.toString + ":" + CLIENT_UNICODE_REQUEST_SIGNATURE
  val CLIENT_UNICODE_REQUEST_METHOD: String = HttpPost.METHOD_NAME
  val CLIENT_UNICODE_REQUEST_PATH = "/resource/path"
  val CLIENT_UNICODE_REQUEST_BODY = "Message with some Unicode characters inside: ș吉ń艾ęتあù"
  val CLIENT_NO_BODY_REQUEST_SIGNATURE: String =
    """NddGBdXnB3/ne3oCmYJQ20mASPHifsI0sG3mt034jjRfjlTafOYJ/kt3RJYk
      |OMLT104GtzTgFfQBeTSJpOrBK/+EK9T0V+JNmjrU6Y9FpcH4p3hB2liooKjH
      |Kfs0L1u3wEG5VOK5xzpjTxO4SQeFQ7GhoAJpNh1p3kcJIPrxRUy3Fbi3FZze
      |WfOevS9yrjidU3713xNsg1d/nJP63b/2zT+mcaZHaDHhQ6IL2z9bKc7H7sBq
      |MSJaqJ4GpuNZPvAd/lkP9/n25w5Jd5fbA+phj+K3MIJWmIETItzS9pt5YgAW
      |W1PjAuZd3w9ugTOXwfWNbc7YIAeCqMRMVp5NLndzww==""".stripMargin
  val CLIENT_NO_BODY_REQUEST_AUTHENTICATION_HEADER: String = "MWS " + EXISTING_CLIENT_APP_UUID.toString + ":" + CLIENT_NO_BODY_REQUEST_SIGNATURE
  val CLIENT_NO_BODY_REQUEST_METHOD: String = HttpGet.METHOD_NAME
  val CLIENT_NO_BODY_REQUEST_PATH = "/resource/path"

  val CLIENT_MCC_TIME_HEADER_VALUE = "1444672122"
  val CLIENT_REQUEST_SIGNATURE_V2: String =
    """et2ht0OkDx20yWlPvOQn1jdTFaT3rS//3t+yl0VqiTgqeMae7x24/UzfD2WQ
      |Bk6o226eQVnCloRjGgq9iLqIIf1wrAFy4CjEHPVCwKOcfbpVQBJYLCyL3Ilz
      |VX6oDmV1Ghukk29mIlgmHGhfHPwGf3vMPvgCQ42GsnAKpRrQ9T4L2IWMM9gk
      |WRAFYDXE3igTM+mWBz3IRrJMLnC2440N/KFNmwh3mVCDxIx/3D4xGhhiGZwA
      |udVbIHmOG045CTSlajxWSNCbClM3nBmAzZn+wRD3DvdvHvDMiAtfVpz7rNLq
      |2rBY2KRNJmPBaAV5ss30FC146jfyg7b8I9fenyauaw==""".stripMargin
  val CLIENT_REQUEST_AUTHENTICATION_HEADER_V2: String = "MWSV2 " + EXISTING_CLIENT_APP_UUID.toString + ":" + CLIENT_REQUEST_SIGNATURE_V2 + ";"

  private val CLIENT_REQUEST_HEADERS = new java.util.HashMap[String, String]()
  CLIENT_REQUEST_HEADERS.put(MAuthRequest.X_MWS_AUTHENTICATION_HEADER_NAME, CLIENT_REQUEST_AUTHENTICATION_HEADER)
  CLIENT_REQUEST_HEADERS.put(MAuthRequest.X_MWS_TIME_HEADER_NAME, CLIENT_X_MWS_TIME_HEADER_VALUE)
  CLIENT_REQUEST_HEADERS.put(MAuthRequest.MCC_AUTHENTICATION_HEADER_NAME, CLIENT_REQUEST_AUTHENTICATION_HEADER_V2)
  CLIENT_REQUEST_HEADERS.put(MAuthRequest.MCC_TIME_HEADER_NAME, CLIENT_MCC_TIME_HEADER_VALUE)

  def getSimpleRequest: MAuthRequest = {
    MAuthRequest.Builder.get
      .withAuthenticationHeaderValue(CLIENT_REQUEST_AUTHENTICATION_HEADER)
      .withTimeHeaderValue(CLIENT_X_MWS_TIME_HEADER_VALUE)
      .withHttpMethod(CLIENT_REQUEST_METHOD)
      .withMessagePayload(CLIENT_REQUEST_BODY.getBytes(StandardCharsets.UTF_8))
      .withResourcePath(CLIENT_REQUEST_PATH)
      .build
  }

  def getSimpleRequestWithWrongSignature: MAuthRequest = {
    MAuthRequest.Builder.get
      .withAuthenticationHeaderValue(CLIENT_REQUEST_AUTHENTICATION_HEADER)
      .withTimeHeaderValue(CLIENT_X_MWS_TIME_HEADER_VALUE)
      .withHttpMethod(CLIENT_REQUEST_METHOD)
      .withMessagePayload(
        (CLIENT_REQUEST_BODY + " this makes this request invalid.")
          .getBytes(StandardCharsets.UTF_8)
      )
      .withResourcePath(CLIENT_REQUEST_PATH)
      .build
  }

  def getRequestWithUnicodeCharactersInBody: MAuthRequest = {
    MAuthRequest.Builder.get
      .withAuthenticationHeaderValue(CLIENT_UNICODE_REQUEST_AUTHENTICATION_HEADER)
      .withTimeHeaderValue(CLIENT_UNICODE_X_MWS_TIME_HEADER_VALUE)
      .withHttpMethod(CLIENT_UNICODE_REQUEST_METHOD)
      .withMessagePayload(CLIENT_UNICODE_REQUEST_BODY.getBytes(StandardCharsets.UTF_8))
      .withResourcePath(CLIENT_UNICODE_REQUEST_PATH)
      .build
  }

  def getRequestWithoutMessageBody: MAuthRequest = {
    MAuthRequest.Builder.get
      .withAuthenticationHeaderValue(CLIENT_NO_BODY_REQUEST_AUTHENTICATION_HEADER)
      .withTimeHeaderValue(CLIENT_NO_BODY_X_MWS_TIME_HEADER_VALUE)
      .withHttpMethod(CLIENT_NO_BODY_REQUEST_METHOD)
      .withResourcePath(CLIENT_NO_BODY_REQUEST_PATH)
      .build
  }

  def getSimpleRequestV2: MAuthRequest = {
    MAuthRequest.Builder.get
      .withAuthenticationHeaderValue(CLIENT_REQUEST_AUTHENTICATION_HEADER_V2)
      .withTimeHeaderValue(CLIENT_MCC_TIME_HEADER_VALUE)
      .withHttpMethod(CLIENT_REQUEST_METHOD)
      .withMessagePayload(CLIENT_REQUEST_BODY.getBytes(StandardCharsets.UTF_8))
      .withResourcePath(CLIENT_REQUEST_PATH)
      .withQueryParameters("")
      .build
  }

  def getRequestWithAllHeaders: MAuthRequest = {
    MAuthRequest.Builder.get
      .withHttpMethod(CLIENT_REQUEST_METHOD)
      .withMauthHeaders(CLIENT_REQUEST_HEADERS)
      .withMessagePayload(CLIENT_REQUEST_BODY.getBytes(StandardCharsets.UTF_8))
      .withResourcePath(CLIENT_REQUEST_PATH)
      .withQueryParameters("")
      .build
  }

  val CLIENT_REQUEST_BINARY_APP_UUID = TestFixtures.APP_UUID_V2
  val CLIENT_REQUEST_BINARY_TIME_HEADER_VALUE = TestFixtures.EPOCH_TIME
  val PUBLIC_KEY2: String = TestFixtures.PUBLIC_KEY_2
  val CLIENT_REQUEST_AUTHENTICATION_BINARY_HEADER_V1: String = "MWS " + TestFixtures.APP_UUID_V2 + ":" + TestFixtures.SIGNATURE_V1_BINARY
  def getRequestWithBinaryBodyV1: MAuthRequest = {
    MAuthRequest.Builder.get
      .withAuthenticationHeaderValue(CLIENT_REQUEST_AUTHENTICATION_BINARY_HEADER_V1)
      .withTimeHeaderValue(CLIENT_REQUEST_BINARY_TIME_HEADER_VALUE)
      .withHttpMethod(TestFixtures.REQUEST_METHOD_V2)
      .withMessagePayload(TestFixtures.BINARY_FILE_BODY)
      .withResourcePath(TestFixtures.REQUEST_PATH_V2)
      .withQueryParameters(TestFixtures.REQUEST_QUERY_PARAMETERS_V2)
      .build
  }

  val CLIENT_REQUEST_AUTHENTICATION_BINARY_HEADER_V2: String = "MWSV2 " + CLIENT_REQUEST_BINARY_APP_UUID + ":" + TestFixtures.SIGNATURE_V2_BINARY
  def getRequestWithBinaryBodyV2: MAuthRequest = {
    MAuthRequest.Builder.get
      .withAuthenticationHeaderValue(CLIENT_REQUEST_AUTHENTICATION_BINARY_HEADER_V2)
      .withTimeHeaderValue(CLIENT_REQUEST_BINARY_TIME_HEADER_VALUE)
      .withHttpMethod(TestFixtures.REQUEST_METHOD_V2)
      .withMessagePayload(TestFixtures.BINARY_FILE_BODY)
      .withResourcePath(TestFixtures.REQUEST_PATH_V2)
      .withQueryParameters(TestFixtures.REQUEST_QUERY_PARAMETERS_V2)
      .build
  }

  val WRONG_CLIENT_REQUEST_SIGNATURE_V2: String =
    """aa2ht0OkDx20yWlPvOQn1jdTFaT3rS//3t+yl0VqiTgqeMae7x24/UzfD2WQ
      |Bk6o226eQVnCloRjGgq9iLqIIf1wrAFy4CjEHPVCwKOcfbpVQBJYLCyL3Ilz
      |VX6oDmV1Ghukk29mIlgmHGhfHPwGf3vMPvgCQ42GsnAKpRrQ9T4L2IWMM9gk
      |WRAFYDXE3igTM+mWBz3IRrJMLnC2440N/KFNmwh3mVCDxIx/3D4xGhhiGZwA
      |udVbIHmOG045CTSlajxWSNCbClM3nBmAzZn+wRD3DvdvHvDMiAtfVpz7rNLq
      |2rBY2KRNJmPBaAV5ss30FC146jfyg7b8I9fenyauaw==""".stripMargin
  private val CLIENT_REQUEST_HEADERS2 = new java.util.HashMap[String, String]()
  CLIENT_REQUEST_HEADERS2.put(MAuthRequest.X_MWS_AUTHENTICATION_HEADER_NAME, CLIENT_REQUEST_AUTHENTICATION_HEADER)
  CLIENT_REQUEST_HEADERS2.put(MAuthRequest.X_MWS_TIME_HEADER_NAME, CLIENT_X_MWS_TIME_HEADER_VALUE)
  CLIENT_REQUEST_HEADERS2
    .put(MAuthRequest.MCC_AUTHENTICATION_HEADER_NAME, "MWSV2 " + EXISTING_CLIENT_APP_UUID.toString + ":" + WRONG_CLIENT_REQUEST_SIGNATURE_V2 + ";")
  CLIENT_REQUEST_HEADERS2.put(MAuthRequest.MCC_TIME_HEADER_NAME, CLIENT_MCC_TIME_HEADER_VALUE)
  def getRequestWithWrongV2Signature: MAuthRequest = {
    MAuthRequest.Builder.get
      .withHttpMethod(CLIENT_REQUEST_METHOD)
      .withMauthHeaders(CLIENT_REQUEST_HEADERS2)
      .withMessagePayload(CLIENT_REQUEST_BODY.getBytes(StandardCharsets.UTF_8))
      .withResourcePath(CLIENT_REQUEST_PATH)
      .withQueryParameters("")
      .build
  }

  def getRequestWithStreamBodyV1: MAuthRequest = {
    MAuthRequest.Builder.get
      .withAuthenticationHeaderValue(CLIENT_REQUEST_AUTHENTICATION_BINARY_HEADER_V1)
      .withTimeHeaderValue(CLIENT_REQUEST_BINARY_TIME_HEADER_VALUE)
      .withHttpMethod(TestFixtures.REQUEST_METHOD_V2)
      .withBodyInputStream(new java.io.ByteArrayInputStream(TestFixtures.BINARY_FILE_BODY))
      .withResourcePath(TestFixtures.REQUEST_PATH_V2)
      .withQueryParameters(TestFixtures.REQUEST_QUERY_PARAMETERS_V2)
      .build
  }

  def getRequestWithStreamBodyV2: MAuthRequest = {
    MAuthRequest.Builder.get
      .withAuthenticationHeaderValue(CLIENT_REQUEST_AUTHENTICATION_BINARY_HEADER_V2)
      .withTimeHeaderValue(CLIENT_REQUEST_BINARY_TIME_HEADER_VALUE)
      .withHttpMethod(TestFixtures.REQUEST_METHOD_V2)
      .withBodyInputStream(new java.io.ByteArrayInputStream(TestFixtures.BINARY_FILE_BODY))
      .withResourcePath(TestFixtures.REQUEST_PATH_V2)
      .withQueryParameters(TestFixtures.REQUEST_QUERY_PARAMETERS_V2)
      .build
  }

  def getRequestWithStreamBodyAndWrongV2Signature: MAuthRequest = {
    MAuthRequest.Builder.get
      .withHttpMethod(CLIENT_REQUEST_METHOD)
      .withMauthHeaders(CLIENT_REQUEST_HEADERS2)
      .withBodyInputStream(new java.io.ByteArrayInputStream(CLIENT_REQUEST_BODY.getBytes(StandardCharsets.UTF_8)))
      .withResourcePath(CLIENT_REQUEST_PATH)
      .withQueryParameters("")
      .build
  }

}
