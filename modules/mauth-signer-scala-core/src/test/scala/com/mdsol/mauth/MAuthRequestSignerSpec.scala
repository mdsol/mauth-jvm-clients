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
import org.apache.http.HttpStatus
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.scalatest.BeforeAndAfterAll
import org.scalatest.concurrent.PatienceConfiguration.Timeout
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import scala.concurrent.duration._

class MAuthRequestSignerSpec extends AnyFlatSpec with Matchers {

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

  val simpleUnsignedRequest: UnsignedRequest = UnsignedRequest(uri = URI_EMPTY_PATH)
  val simpleNewUnsignedRequest: NewUnsignedRequest =
    NewUnsignedRequest.fromStringBodyUtf8(httpMethod = "GET", uri = URI_EMPTY_PATH, body = "", headers = Map.empty)

  val unsignedRequest: NewUnsignedRequest =
    NewUnsignedRequest.fromStringBodyUtf8(httpMethod = "GET", uri = URI_EMPTY_PATH_WITH_PARAM, body = SIMPLE_REQUEST_BODY, headers = Map.empty)
  "MAuthRequestSigner" should "add time header to a request for V1" in {
    signer.signRequest(simpleUnsignedRequest).getOrElse(fail("signRequest unexpectedly failed")).timeHeader shouldBe EXPECTED_TIME_HEADER_1
  }

  it should "add authentication header to a request for V1" in {
    signer.signRequest(simpleUnsignedRequest).getOrElse(fail("signRequest unexpectedly failed")).authHeader shouldBe EXPECTED_AUTH_NO_BODY_V1
  }

  it should "add authentication header to a request with body for V1" in {
    signer
      .signRequest(UnsignedRequest(uri = URI_EMPTY_PATH, body = Some(SIMPLE_REQUEST_BODY)))
      .getOrElse(fail("signRequest unexpectedly failed"))
      .authHeader shouldBe EXPECTED_AUTH_SIMPLE_BODY_V1
  }

  it should "add authentication header to a request" in {
    val authHeaders = signer.signRequest(simpleNewUnsignedRequest).mauthHeaders
    authHeaders(MAuthRequest.X_MWS_AUTHENTICATION_HEADER_NAME) shouldBe EXPECTED_AUTH_NO_BODY_V1
    authHeaders(MAuthRequest.MCC_AUTHENTICATION_HEADER_NAME) shouldBe EXPECTED_AUTH_NO_BODY_V2
  }

  it should "add authentication header to a request with body" in {
    signer
      .signRequest(NewUnsignedRequest.fromStringBodyUtf8(httpMethod = "GET", uri = URI_EMPTY_PATH, body = SIMPLE_REQUEST_BODY, headers = Map.empty))
      .mauthHeaders(MAuthRequest.X_MWS_AUTHENTICATION_HEADER_NAME) shouldBe EXPECTED_AUTH_SIMPLE_BODY_V1
  }

  it should "add authentication header to a request with body and params" in {
    val authHeaders = signer.signRequest(unsignedRequest).mauthHeaders
    authHeaders(MAuthRequest.X_MWS_AUTHENTICATION_HEADER_NAME) shouldBe EXPECTED_AUTH_BODY_AND_PARAM_V1
    authHeaders(MAuthRequest.MCC_AUTHENTICATION_HEADER_NAME) shouldBe EXPECTED_AUTH_BODY_AND_PARAM_V2
  }

  "MAuthRequestSigner with V2 only enabled" should "add time header to a request for V2 only " in {
    val authHeaders = signerV2.signRequest(simpleNewUnsignedRequest).mauthHeaders
    authHeaders.get(MAuthRequest.X_MWS_TIME_HEADER_NAME) shouldBe None
    authHeaders(MAuthRequest.MCC_TIME_HEADER_NAME) shouldBe EXPECTED_TIME_HEADER_1
  }

  it should "add authentication header to a request for V2 only" in {
    val authHeaders = signerV2.signRequest(simpleNewUnsignedRequest).mauthHeaders
    authHeaders.get(MAuthRequest.X_MWS_AUTHENTICATION_HEADER_NAME) shouldBe None
    authHeaders(MAuthRequest.MCC_AUTHENTICATION_HEADER_NAME) shouldBe EXPECTED_AUTH_NO_BODY_V2
  }

  it should "add authentication header to a request with body and params for V2 only" in {
    val authHeaders = signerV2.signRequest(unsignedRequest).mauthHeaders
    authHeaders.get(MAuthRequest.X_MWS_AUTHENTICATION_HEADER_NAME) shouldBe None
    authHeaders(MAuthRequest.MCC_AUTHENTICATION_HEADER_NAME) shouldBe EXPECTED_AUTH_BODY_AND_PARAM_V2
  }

  it should "add authentication header to a request for V2 with the encoded-normalize path" in {
    val TEST_UUID = APP_UUID_V2
    val EXPECTED_SIGNATURE_V2 = SIGNATURE_NORMALIZE_PATH_V2
    val EXPECTED_AUTHENTICATION_HEADER = s"""MWSV2 $TEST_UUID:$EXPECTED_SIGNATURE_V2;"""
    val eTimeProvider: EpochTimeProvider = new EpochTimeProvider() { override def inSeconds(): Long = EPOCH_TIME.toLong }
    val newSigner: MAuthRequestSigner =
      MAuthRequestSigner(UUID.fromString(TEST_UUID), PRIVATE_KEY_2, eTimeProvider, java.util.Arrays.asList(MAuthVersion.MWSV2))

    newSigner
      .signRequest(
        NewUnsignedRequest.fromStringBodyUtf8(httpMethod = "GET", uri = new URI(REQUEST_NORMALIZE_PATH), body = "", headers = Map.empty)
      )
      .mauthHeaders(MAuthRequest.MCC_AUTHENTICATION_HEADER_NAME) shouldBe EXPECTED_AUTHENTICATION_HEADER
  }


  "MAuthRequestSigner with V1 only required" should "add mauth headers to a request for V1 only " in {
    val authHeaders = signerV1.signRequest(simpleNewUnsignedRequest).mauthHeaders
    authHeaders.size shouldEqual 2
    authHeaders(MAuthRequest.X_MWS_TIME_HEADER_NAME) shouldBe EXPECTED_TIME_HEADER_1
    authHeaders(MAuthRequest.X_MWS_AUTHENTICATION_HEADER_NAME) shouldBe EXPECTED_AUTH_NO_BODY_V1
  }

}
