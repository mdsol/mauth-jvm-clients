package com.mdsol.mauth

import java.nio.charset.StandardCharsets
import java.security.Security

import com.mdsol.mauth.test.utils.FakeMAuthServer.EXISTING_CLIENT_APP_UUID
import com.mdsol.mauth.test.utils.FixturesLoader
import com.mdsol.mauth.util.EpochTimeProvider
import org.apache.http.client.methods.{HttpGet, HttpPost, HttpPut}
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.scalamock.scalatest.MockFactory
import org.scalatest.BeforeAndAfterAll
import org.scalatest.flatspec.AnyFlatSpec

trait RequestAuthenticatorBaseSpec extends AnyFlatSpec with BeforeAndAfterAll with MockFactory {

  val CLIENT_X_MWS_TIME_HEADER_VALUE = "1444672122"
  val CLIENT_UNICODE_X_MWS_TIME_HEADER_VALUE = "1444748974"
  val CLIENT_NO_BODY_X_MWS_TIME_HEADER_VALUE = "1424700000"
  val PUBLIC_KEY: String = FixturesLoader.getPublicKey
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

  val mockEpochTimeProvider: EpochTimeProvider = mock[EpochTimeProvider]

  private val CLIENT_REQUEST_HEADERS = new java.util.HashMap[String, String]()
  CLIENT_REQUEST_HEADERS.put(MAuthRequest.X_MWS_AUTHENTICATION_HEADER_NAME, CLIENT_REQUEST_AUTHENTICATION_HEADER)
  CLIENT_REQUEST_HEADERS.put(MAuthRequest.X_MWS_TIME_HEADER_NAME, CLIENT_X_MWS_TIME_HEADER_VALUE)
  CLIENT_REQUEST_HEADERS.put(MAuthRequest.MCC_AUTHENTICATION_HEADER_NAME, CLIENT_REQUEST_AUTHENTICATION_HEADER_V2)
  CLIENT_REQUEST_HEADERS.put(MAuthRequest.MCC_TIME_HEADER_NAME, CLIENT_MCC_TIME_HEADER_VALUE)

  override protected def beforeAll() {
    Security.addProvider(new BouncyCastleProvider)
  }

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

  val CLIENT_REQUEST_BINARY_APP_UUID = "5ff4257e-9c16-11e0-b048-0026bbfffe5e"
  val CLIENT_X_MWS_TIME_HEADER_BINARY_VALUE = "1309891855"
  val CLIENT_REQUEST_BINARY_PATH = "/v1/pictures"
  val PUBLIC_KEY2: String = FixturesLoader.getPublicKey2
  val CLIENT_REQUEST_SIGNATURE_BINARY_V1: String =
    ("hDKYDRnzPFL2gzsru4zn7c7E7KpEvexeF4F5IR+puDxYXrMmuT2/fETZty5NkG" +
      "GTZQ1nI6BTYGQGsU/73TkEAm7SvbJZcB2duLSCn8H5D0S1cafory1gnL1TpMP" +
      "BlY8J/lq/Mht2E17eYw+P87FcpvDShINzy8GxWHqfquBqO8ml4XtirVEtAlI0" +
      "xlkAsKkVq4nj7rKZUMS85mzogjUAJn3WgpGCNXVU+EK+qElW5QXk3I9uozByZ" +
      "hwBcYt5Cnlg15o99+53wKzMMmdvFmVjA1DeUaSO7LMIuw4ZNLVdDcHJx7ZSpA" +
      "KZ/EA34u1fYNECFcw5CSKOjdlU7JFr4o8Phw==").stripMargin
  val CLIENT_REQUEST_AUTHENTICATION_BINARY_HEADER: String = "MWS " + CLIENT_REQUEST_BINARY_APP_UUID + ":" + CLIENT_REQUEST_SIGNATURE_BINARY_V1
  def getRequestWithBinaryBodyV1: MAuthRequest = {
    MAuthRequest.Builder.get
      .withAuthenticationHeaderValue(CLIENT_REQUEST_AUTHENTICATION_BINARY_HEADER)
      .withTimeHeaderValue(CLIENT_X_MWS_TIME_HEADER_BINARY_VALUE)
      .withHttpMethod(HttpPut.METHOD_NAME)
      .withMessagePayload(FixturesLoader.getBinaryFileBody)
      .withResourcePath(CLIENT_REQUEST_BINARY_PATH)
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

}