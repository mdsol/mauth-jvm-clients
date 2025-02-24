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
import com.mdsol.mauth.test.utils.TestFixtures._
import com.mdsol.mauth.util.EpochTimeProvider
import org.apache.hc.core5.http.HttpStatus
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.scalatest.BeforeAndAfterAll
import org.scalatest.concurrent.PatienceConfiguration.Timeout
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import scala.concurrent.duration._

class MAuthRequestAkkaSignerSpec extends AnyFlatSpec with Matchers with HttpClient with BeforeAndAfterAll with ScalaFutures {

  implicit val system: ActorSystem = ActorSystem()
  var service = new WireMockServer(wireMockConfig.dynamicPort)

  Security.addProvider(new BouncyCastleProvider)

  val CONST_EPOCH_TIME_PROVIDER: EpochTimeProvider = new EpochTimeProvider() { override def inSeconds(): Long = EXPECTED_TIME_HEADER_1.toLong }

  val signer: MAuthRequestSigner = new MAuthRequestSigner(
    UUID.fromString(APP_UUID_1),
    PRIVATE_KEY_1,
    CONST_EPOCH_TIME_PROVIDER,
    SignerConfiguration.ALL_SIGN_VERSIONS
  )

  val signerV2: MAuthRequestSigner = new MAuthRequestSigner(
    UUID.fromString(APP_UUID_1),
    PRIVATE_KEY_1,
    CONST_EPOCH_TIME_PROVIDER,
    java.util.Arrays.asList[MAuthVersion](MAuthVersion.MWSV2)
  )

  val signerV1: MAuthRequestSigner = new MAuthRequestSigner(
    UUID.fromString(APP_UUID_1),
    PRIVATE_KEY_1,
    CONST_EPOCH_TIME_PROVIDER,
    java.util.Arrays.asList[MAuthVersion](MAuthVersion.MWS)
  )

  override protected def beforeAll(): Unit =
    service.start()

  override protected def afterAll(): Unit =
    service.stop()

  val simpleUnsignedRequest: UnsignedRequest = UnsignedRequest(uri = URI_EMPTY_PATH)
  val simpleNewUnsignedRequest: NewUnsignedRequest =
    NewUnsignedRequest.fromStringBodyUtf8(httpMethod = "GET", uri = URI_EMPTY_PATH, body = "", headers = Map.empty)

  val unsignedRequest: NewUnsignedRequest =
    NewUnsignedRequest.fromStringBodyUtf8(httpMethod = "GET", uri = URI_EMPTY_PATH_WITH_PARAM, body = SIMPLE_REQUEST_BODY, headers = Map.empty)
  "MAuthRequestSigner" should "add time header to a request for V1" in {
    signer.signRequest(simpleUnsignedRequest).getOrElse(fail("signRequest unexpectedly failed")).timeHeader shouldBe EXPECTED_TIME_HEADER_1
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
