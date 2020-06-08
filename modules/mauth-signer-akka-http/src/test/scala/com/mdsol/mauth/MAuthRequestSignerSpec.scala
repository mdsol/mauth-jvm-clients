package com.mdsol.mauth

import java.net.URI
import java.security.Security
import java.util.UUID

import akka.actor.ActorSystem
import akka.http.scaladsl.model._
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, equalTo, post, urlPathEqualTo}
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import com.mdsol.mauth.http.HttpClient
import com.mdsol.mauth.http.Implicits._
import com.mdsol.mauth.models.{UnsignedRequest => NewUnsignedRequest}
import com.mdsol.mauth.test.utils.FixturesLoader
import com.mdsol.mauth.util.EpochTimeProvider
import org.apache.http.HttpStatus
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.scalatest.BeforeAndAfterAll
import org.scalatest.concurrent.PatienceConfiguration.Timeout
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import scala.concurrent.duration._

class MAuthRequestSignerSpec extends AnyFlatSpec with Matchers with HttpClient with BeforeAndAfterAll with ScalaFutures {

  implicit val system: ActorSystem = ActorSystem()
  var service = new WireMockServer(wireMockConfig.dynamicPort)
  val TIME_CONSTANT = 1509041057L
  val epochTimeProvider: EpochTimeProvider = new EpochTimeProvider() { override def inSeconds(): Long = TIME_CONSTANT }
  val testUUID = "2a6790ab-f6c6-45be-86fc-9e9be76ec12a"

  Security.addProvider(new BouncyCastleProvider)

  val signer: MAuthRequestSigner = MAuthRequestSigner(
    UUID.fromString(testUUID),
    FixturesLoader.getPrivateKey,
    epochTimeProvider
  )

  val signerV2: MAuthRequestSigner = MAuthRequestSigner(
    UUID.fromString(testUUID),
    FixturesLoader.getPrivateKey,
    epochTimeProvider,
    v2OnlySignRequests = true
  )

  override protected def beforeAll(): Unit =
    service.start()

  override protected def afterAll(): Unit =
    service.stop()

  val EXPECTED_GET_TIME_HEADER = "1509041057"

  val simpleUnsignedRequest: UnsignedRequest = UnsignedRequest(uri = new URI("/"))
  val simpleNewUnsignedRequest: NewUnsignedRequest =
    NewUnsignedRequest.fromStringBodyUtf8(httpMethod = "GET", uri = new URI("/"), body = "", headers = Map.empty)

  val EXPECTED_GET_X_MWS_AUTHENTICATION_HEADER_FOR_SIMPLE_REQUEST: String =
    s"""MWS $testUUID:ih3xq6OvQ2/D5ktPDaZ4F6tanzdn2XGzZ+KOaFXC+YKVjNcSCfUiKB6T/552K3AmKm/yZF4rdEOps
       |MZ0QkuFqEZdwQ8R3iWUwdrNPsmNXSVvF50pRAlcI77UP5gUKV01xjZxfZ/M/vhzVn513bAgJ6CM8X4dtG20ki5xLsO3
       |5e2eZs5i9IwA/hEaKSm/PH2pEHwxz5c9MMGtHiFgzgXGacziVn0fr2c6X5jb3cDjHnfNVX8o57kFjL5E0YOoeEKDwHy
       |flGhbfFNZys29jo83JCK2MQj9s+fZq5NsgmwuACRE6BnqKSPqwDWN4OK3N/iPcTwCsMKz/c5/3CEbMTlH8A==""".stripMargin.replaceAll("\n", "")
  val EXPECTED_GET_MCC_AUTHENTICATION_HEADER_FOR_SIMPLE_REQUEST: String =
    s"""MWSV2 $testUUID:h0MJYf5/zlX9VqqchANLr7XUln0RydMV4msZSXzLq2sbr3X+TGeJ60K9ZSlSuRrzyHbzzwuZABA
       |3P2j3l9t+gyBC1c/JSa8mldMrIXXYzp0lYLxLkghH09hm3k0pEW2la94K/Num3xgNymn6D/B9dJ1onRIgl+T+e/m4k6
       |T3apKHcV/6cJ9asm+jDjzB8OuCVWVsLZQKQbtiydUYNisYerKVxWPLs9SHNZ6GmAqq4ZCCpyEQZuMNF6cMmXgQ0Pxe9
       |X/yNA1Xc3Fakuga47lUQ6Bn7xvhkH6P+ZP0k4U7kidziXpxpkDts8fEXTpkvFX0PR7vaxjbMZzWsU413jyNsw==;""".stripMargin.replaceAll("\n", "")

  val unsignedRequest: NewUnsignedRequest =
    NewUnsignedRequest.fromStringBodyUtf8(httpMethod = "GET", uri = new URI("/" + "?key2=data2&key1=data1"), body = "Request Body", headers = Map.empty)
  val EXPECTED_GET_X_MWS_AUTHENTICATION_HEADER: String =
    s"""MWS $testUUID:OoxiQ/Z6EjTUAoAGNKD5FS6ka+9IcWW5rtuzbXRDLRGj4pzSdeI0FPIlT0E/ZR96xR0a5EJlJ3E
       |8usr5qas/uzNEDajAqpjqOaO4m3j+4juXt0QrdBvj3sgStD6ozOJrfhyeSWvFp3d9SBx8tPkPrqv6z5ewQliaSOaI
       |20yir4+RStwj6P7j/5/ZlDRMBEFBiFuAyAWMAbKnefRwK+0yUqO9pEBQx43YqBzs+Xb9sTM0hKd5IohAW8O8xj1coB
       |YP/NGRvhM5Z+VMXnbRXwkqUlEXIDvZ3fKjPNGEQxo+m9oFH1dLI8oGI9xoC9P3liwUqY5h+g+hSQ4KLIfDm0qvLQ==""".stripMargin.replaceAll("\n", "")
  val EXPECTED_GET_MCC_AUTHENTICATION_HEADER: String =
    s"""MWSV2 $testUUID:n5+io+SgpPMgatLarleDkX18r1ZVBtp7YWgu3yeP0k/P8otp4ThEtBJ6Du3b2Pet+7xlkfK90
       |RXrcwiKA0SS8vpPX8nCtLa92hE3G1e0A41Cn00MuasVwV7JlkQeffJH8qQjvapwRsQ9dbFTPOktS4u0fm/7L9hI6k
       |m99lqCP72i0tP7vGCst4Gc1OewGMR+60FUNR7eN66z8wbeXxX5gzMNGpppP/3P2YROGkONlsxbd1UxrEN62r6yQBF
       |i9hTFF0FCqDM63UiLxTt3ooTpj4iUx/3htvPJ2AlSW5TaoviQUjQFYdb+CB6xDi0LFp93V5289lEXdPOVCULUGesqDA==;""".stripMargin.replaceAll("\n", "")

  "MAuthRequestSigner" should "add time header to a request for V1" in {
    signer.signRequest(simpleUnsignedRequest).right.get.timeHeader shouldBe EXPECTED_GET_TIME_HEADER
  }

  it should "add authentication header to a request for V1" in {
    signer.signRequest(simpleUnsignedRequest).right.get.authHeader shouldBe EXPECTED_GET_X_MWS_AUTHENTICATION_HEADER_FOR_SIMPLE_REQUEST
  }

  it should "add authentication header to a request with body for V1" in {
    signer.signRequest(UnsignedRequest(uri = new URI("/"), body = Some("Request Body"))).right.get.authHeader shouldBe "MWS " +
      "2a6790ab-f6c6-45be-86fc-9e9be76ec12a:OoxiQ/Z6EjTUAoAGNKD5FS6ka+9IcWW5rtuzbXRDLRGj4pzSdeI0FPIlT0E/ZR96xR0a5EJlJ3E8usr" +
      "5qas/uzNEDajAqpjqOaO4m3j+4juXt0QrdBvj3sgStD6ozOJrfhyeSWvFp3d9SBx8tPkPrqv6z5ewQliaSOaI20yir4+RStwj6P7j/5/ZlDRMBEFBiFuA" +
      "yAWMAbKnefRwK+0yUqO9pEBQx43YqBzs+Xb9sTM0hKd5IohAW8O8xj1coBYP/NGRvhM5Z+VMXnbRXwkqUlEXIDvZ3fKjPNGEQxo+m9oFH1dLI8oGI9xoC9P3liwUqY5h+g+hSQ4KLIfDm0qvLQ=="
  }

  it should "add authentication header to a request" in {
    val authHeaders = signer.signRequest(simpleNewUnsignedRequest).mauthHeaders
    authHeaders(MAuthRequest.X_MWS_AUTHENTICATION_HEADER_NAME) shouldBe EXPECTED_GET_X_MWS_AUTHENTICATION_HEADER_FOR_SIMPLE_REQUEST
    authHeaders(MAuthRequest.MCC_AUTHENTICATION_HEADER_NAME) shouldBe EXPECTED_GET_MCC_AUTHENTICATION_HEADER_FOR_SIMPLE_REQUEST
  }

  it should "add authentication header to a request with body" in {
    signer
      .signRequest(NewUnsignedRequest.fromStringBodyUtf8(httpMethod = "GET", uri = new URI("/"), body = "Request Body", headers = Map.empty))
      .mauthHeaders(MAuthRequest.X_MWS_AUTHENTICATION_HEADER_NAME) shouldBe "MWS " +
      "2a6790ab-f6c6-45be-86fc-9e9be76ec12a:OoxiQ/Z6EjTUAoAGNKD5FS6ka+9IcWW5rtuzbXRDLRGj4pzSdeI0FPIlT0E/ZR96xR0a5EJlJ3E8usr" +
      "5qas/uzNEDajAqpjqOaO4m3j+4juXt0QrdBvj3sgStD6ozOJrfhyeSWvFp3d9SBx8tPkPrqv6z5ewQliaSOaI20yir4+RStwj6P7j/5/ZlDRMBEFBiFuA" +
      "yAWMAbKnefRwK+0yUqO9pEBQx43YqBzs+Xb9sTM0hKd5IohAW8O8xj1coBYP/NGRvhM5Z+VMXnbRXwkqUlEXIDvZ3fKjPNGEQxo+m9oFH1dLI8oGI9xoC9P3liwUqY5h+g+hSQ4KLIfDm0qvLQ=="
  }

  it should "add authentication header to a request with body and params" in {
    val authHeaders = signer.signRequest(unsignedRequest).mauthHeaders
    authHeaders(MAuthRequest.X_MWS_AUTHENTICATION_HEADER_NAME) shouldBe EXPECTED_GET_X_MWS_AUTHENTICATION_HEADER
    authHeaders(MAuthRequest.MCC_AUTHENTICATION_HEADER_NAME) shouldBe EXPECTED_GET_MCC_AUTHENTICATION_HEADER
  }

  "MAuthRequestSigner with V2 only enabled" should "add time header to a request for V2 only " in {
    val authHeaders = signerV2.signRequest(simpleNewUnsignedRequest).mauthHeaders
    authHeaders.get(MAuthRequest.X_MWS_TIME_HEADER_NAME) shouldBe None
    authHeaders(MAuthRequest.MCC_TIME_HEADER_NAME) shouldBe EXPECTED_GET_TIME_HEADER
  }

  it should "add authentication header to a request for V2 only" in {
    val authHeaders = signerV2.signRequest(simpleNewUnsignedRequest).mauthHeaders
    authHeaders.get(MAuthRequest.X_MWS_AUTHENTICATION_HEADER_NAME) shouldBe None
    authHeaders(MAuthRequest.MCC_AUTHENTICATION_HEADER_NAME) shouldBe EXPECTED_GET_MCC_AUTHENTICATION_HEADER_FOR_SIMPLE_REQUEST
  }

  it should "add authentication header to a request with body and params for V2 only" in {
    val authHeaders = signerV2.signRequest(unsignedRequest).mauthHeaders
    authHeaders.get(MAuthRequest.X_MWS_AUTHENTICATION_HEADER_NAME) shouldBe None
    authHeaders(MAuthRequest.MCC_AUTHENTICATION_HEADER_NAME) shouldBe EXPECTED_GET_MCC_AUTHENTICATION_HEADER
  }

  it should "add authentication header to a request for V2 with the encoded-normalize path" in {
    val TEST_UUID = FixturesLoader.APP_UUID_V2
    val EXPECTED_SIGNATURE_V2 = FixturesLoader.SIGNATURE_NORMALIZE_PATH_V2
    val EXPECTED_AUTHENTICATION_HEADER = s"""MWSV2 $TEST_UUID:$EXPECTED_SIGNATURE_V2;"""
    val eTimeProvider: EpochTimeProvider = new EpochTimeProvider() { override def inSeconds(): Long = FixturesLoader.EPOCH_TIME_V2.toLong }
    val newSigner: MAuthRequestSigner = MAuthRequestSigner(UUID.fromString(TEST_UUID), FixturesLoader.getPrivateKey2, eTimeProvider, v2OnlySignRequests = true)

    newSigner
      .signRequest(
        NewUnsignedRequest.fromStringBodyUtf8(httpMethod = "GET", uri = new URI(FixturesLoader.REQUEST_NORMALIZE_PATH), body = "", headers = Map.empty)
      )
      .mauthHeaders(MAuthRequest.MCC_AUTHENTICATION_HEADER_NAME) shouldBe EXPECTED_AUTHENTICATION_HEADER
  }

  it should "correctly send a customized content-type header" in {
    service.stubFor(
      post(urlPathEqualTo(s"/v1/test"))
        .willReturn(aResponse().withStatus(HttpStatus.SC_OK))
        .withHeader("Content-type", equalTo("application/json"))
    )

    val simpleNewUnsignedRequest =
      NewUnsignedRequest
        .fromStringBodyUtf8(
          httpMethod = "POST",
          uri = new URI(s"${service.baseUrl()}/v1/test"),
          body = "",
          headers = Map("Content-Type" -> ContentTypes.`application/json`.toString())
        )

    whenReady(HttpClient.call(signerV2.signRequest(simpleNewUnsignedRequest).toAkkaHttpRequest), timeout = Timeout(5.seconds)) { response =>
      response.status shouldBe StatusCodes.OK
    }
  }

  it should "correctly send the default content type (text/plain UTF-8) when content type not specified" in {
    service.stubFor(
      post(urlPathEqualTo(s"/v1/test"))
        .willReturn(aResponse().withStatus(HttpStatus.SC_OK))
        .withHeader("Content-type", equalTo(ContentTypes.`text/plain(UTF-8)`.toString()))
    )

    val simpleNewUnsignedRequest =
      NewUnsignedRequest
        .fromStringBodyUtf8(httpMethod = "POST", uri = new URI(s"${service.baseUrl()}/v1/test"), body = "", headers = Map())

    whenReady(HttpClient.call(signerV2.signRequest(simpleNewUnsignedRequest).toAkkaHttpRequest), timeout = Timeout(5.seconds)) { response =>
      response.status shouldBe StatusCodes.OK
    }
  }
}
