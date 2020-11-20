package com.mdsol.mauth.scaladsl.utils

import java.util.UUID

import com.mdsol.mauth.RequestAuthenticatorBaseSpec
import com.mdsol.mauth.exception.MAuthValidationException
import com.mdsol.mauth.scaladsl.RequestAuthenticator
import com.mdsol.mauth.test.utils.FakeMAuthServer.EXISTING_CLIENT_APP_UUID
import com.mdsol.mauth.util.MAuthKeysHelper
import org.scalamock.scalatest.MockFactory
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._

class RequestAuthenticatorSpec extends AnyFlatSpec with RequestAuthenticatorBaseSpec with Matchers with ScalaFutures with MockFactory {

  private implicit val requestValidationTimeout: Duration = 10.seconds

  behavior of "RequestAuthenticator Scala"

  it should "authenticate a valid request" in clientContext { client =>
    //noinspection ConvertibleToMethodValue
    (mockEpochTimeProvider.inSeconds _: () => Long).expects().returns(CLIENT_X_MWS_TIME_HEADER_VALUE.toLong + 3)
    val authenticator = new RequestAuthenticator(client, mockEpochTimeProvider)

    whenReady(authenticator.authenticate(getSimpleRequest))(validationResult => validationResult shouldBe true)
  }

  it should "authenticate a request with unicode chars in body" in clientContext { client =>
    //noinspection ConvertibleToMethodValue
    (mockEpochTimeProvider.inSeconds _: () => Long).expects().returns(CLIENT_UNICODE_X_MWS_TIME_HEADER_VALUE.toLong + 3)
    val authenticator = new RequestAuthenticator(client, mockEpochTimeProvider)

    whenReady(authenticator.authenticate(getRequestWithUnicodeCharactersInBody))(validationResult => validationResult shouldBe true)
  }

  it should "authenticate a request without any body" in clientContext { client =>
    //noinspection ConvertibleToMethodValue
    (mockEpochTimeProvider.inSeconds _: () => Long).expects().returns(CLIENT_NO_BODY_X_MWS_TIME_HEADER_VALUE.toLong + 3)
    val authenticator = new RequestAuthenticator(client, mockEpochTimeProvider)

    whenReady(authenticator.authenticate(getRequestWithoutMessageBody))(validationResult => validationResult shouldBe true)
  }

  it should "not authenticate an invalid request" in clientContext { client =>
    //noinspection ConvertibleToMethodValue
    (mockEpochTimeProvider.inSeconds _: () => Long).expects().returns(CLIENT_X_MWS_TIME_HEADER_VALUE.toLong + 3)
    val authenticator = new RequestAuthenticator(client, mockEpochTimeProvider)
    val invalidRequest = getSimpleRequestWithWrongSignature

    whenReady(authenticator.authenticate(invalidRequest))(validationResult => validationResult shouldBe false)
  }

  it should "not authenticate a request after timeout period passed" in {
    //noinspection ConvertibleToMethodValue
    (mockEpochTimeProvider.inSeconds _: () => Long).expects().returns(CLIENT_UNICODE_X_MWS_TIME_HEADER_VALUE.toLong + 600)
    val authenticator = new RequestAuthenticator(mock[ClientPublicKeyProvider], mockEpochTimeProvider)

    whenReady(authenticator.authenticate(getRequestWithUnicodeCharactersInBody).failed) {
      case e: MAuthValidationException => e.getMessage shouldBe "MAuth request validation failed because of timeout 10 seconds"
      case _                           => fail("should not be here")
    }
  }

  it should "authenticate a valid request with V2 headers" in clientContext { client =>
    //noinspection ConvertibleToMethodValue
    (mockEpochTimeProvider.inSeconds _: () => Long).expects().returns(CLIENT_X_MWS_TIME_HEADER_VALUE.toLong + 3)
    val authenticator = new RequestAuthenticator(client, mockEpochTimeProvider)

    whenReady(authenticator.authenticate(getSimpleRequestV2))(validationResult => validationResult shouldBe true)
  }

  it should "authenticate a valid request with V2 headers only if V2 only enabled" in clientContext { client =>
    //noinspection ConvertibleToMethodValue
    (mockEpochTimeProvider.inSeconds _: () => Long).expects().returns(CLIENT_X_MWS_TIME_HEADER_VALUE.toLong + 3)
    val authenticator = new RequestAuthenticator(client, mockEpochTimeProvider, true)

    val result = authenticator.authenticate(getSimpleRequestV2).futureValue
    result shouldBe true
  }

  it should "authenticate a valid request with the both V1 and V2 headers provided" in clientContext { client =>
    //noinspection ConvertibleToMethodValue
    (mockEpochTimeProvider.inSeconds _: () => Long).expects().returns(CLIENT_X_MWS_TIME_HEADER_VALUE.toLong + 3)
    val authenticator = new RequestAuthenticator(client, mockEpochTimeProvider)

    whenReady(authenticator.authenticate(getRequestWithAllHeaders))(validationResult => validationResult shouldBe true)
  }

  it should "reject a request with V1 headers when V2 only is enabled" in {
    //noinspection ConvertibleToMethodValue
    (mockEpochTimeProvider.inSeconds _: () => Long).expects().returns(CLIENT_X_MWS_TIME_HEADER_VALUE.toLong + 3)
    val authenticator = new RequestAuthenticator(mock[ClientPublicKeyProvider], mockEpochTimeProvider, true)

    whenReady(authenticator.authenticate(getSimpleRequest).failed) {
      case e: MAuthValidationException => e.getMessage shouldBe "The service requires mAuth v2 authentication headers."
      case _                           => fail("should not be here")
    }
  }

  it should "authenticate a valid request with binary payload" in {
    val client: ClientPublicKeyProvider = mock[ClientPublicKeyProvider]
    (client.getPublicKey _).expects(UUID.fromString(CLIENT_REQUEST_BINARY_APP_UUID)).returns(Future(Some(MAuthKeysHelper.getPublicKeyFromString(PUBLIC_KEY2))))
    (mockEpochTimeProvider.inSeconds _: () => Long).expects().returns(CLIENT_REQUEST_BINARY_TIME_HEADER_VALUE.toLong + 3)
    val authenticator = new RequestAuthenticator(client, mockEpochTimeProvider)

    whenReady(authenticator.authenticate(getRequestWithBinaryBodyV1))(validationResult => validationResult shouldBe true)
  }

  it should "authenticate a valid request with binary payload for V2" in {
    val client: ClientPublicKeyProvider = mock[ClientPublicKeyProvider]
    (client.getPublicKey _).expects(UUID.fromString(CLIENT_REQUEST_BINARY_APP_UUID)).returns(Future(Some(MAuthKeysHelper.getPublicKeyFromString(PUBLIC_KEY2))))
    (mockEpochTimeProvider.inSeconds _: () => Long).expects().returns(CLIENT_REQUEST_BINARY_TIME_HEADER_VALUE.toLong + 5)
    val authenticator = new RequestAuthenticator(client, mockEpochTimeProvider)

    whenReady(authenticator.authenticate(getRequestWithBinaryBodyV2))(validationResult => validationResult shouldBe true)
  }

  it should "validate the request with the validated V1 headers and wrong V2 signature" in clientContext { client =>
    //noinspection ConvertibleToMethodValue
    (mockEpochTimeProvider.inSeconds _: () => Long).expects().returns(CLIENT_X_MWS_TIME_HEADER_VALUE.toLong + 3)
    val authenticator = new RequestAuthenticator(client, mockEpochTimeProvider)

    whenReady(authenticator.authenticate(getRequestWithWrongV2Signature))(validationResult => validationResult shouldBe true)
  }

  it should "fail validating request with validated V1 headers and wrong V2 signature if V2 only is enabled" in clientContext { client =>
    //noinspection ConvertibleToMethodValue
    (mockEpochTimeProvider.inSeconds _: () => Long).expects().returns(CLIENT_X_MWS_TIME_HEADER_VALUE.toLong + 3)
    val authenticator = new RequestAuthenticator(client, mockEpochTimeProvider, true)

    whenReady(authenticator.authenticate(getRequestWithWrongV2Signature))(validationResult => validationResult shouldBe false)
  }

  "When payload is inputstream" should "authenticate a valid request for V1" in {
    val client: ClientPublicKeyProvider = mock[ClientPublicKeyProvider]
    (client.getPublicKey _).expects(UUID.fromString(CLIENT_REQUEST_BINARY_APP_UUID)).returns(Future(Some(MAuthKeysHelper.getPublicKeyFromString(PUBLIC_KEY2))))
    (mockEpochTimeProvider.inSeconds _: () => Long).expects().returns(CLIENT_REQUEST_BINARY_TIME_HEADER_VALUE.toLong + 3)
    val authenticator = new RequestAuthenticator(client, mockEpochTimeProvider)

    whenReady(authenticator.authenticate(getRequestWithStreamBodyV1))(validationResult => validationResult shouldBe true)
  }

  it should "authenticate a valid request for V2" in {
    val client: ClientPublicKeyProvider = mock[ClientPublicKeyProvider]
    (client.getPublicKey _).expects(UUID.fromString(CLIENT_REQUEST_BINARY_APP_UUID)).returns(Future(Some(MAuthKeysHelper.getPublicKeyFromString(PUBLIC_KEY2))))
    (mockEpochTimeProvider.inSeconds _: () => Long).expects().returns(CLIENT_REQUEST_BINARY_TIME_HEADER_VALUE.toLong + 5)
    val authenticator = new RequestAuthenticator(client, mockEpochTimeProvider)

    whenReady(authenticator.authenticate(getRequestWithStreamBodyV2))(validationResult => validationResult shouldBe true)
  }

  it should "fail validating the request with the validated V1 headers and wrong V2 signature" in clientContext { client =>
    //noinspection ConvertibleToMethodValue
    (mockEpochTimeProvider.inSeconds _: () => Long).expects().returns(CLIENT_X_MWS_TIME_HEADER_VALUE.toLong + 3)
    val authenticator = new RequestAuthenticator(client, mockEpochTimeProvider)

    whenReady(authenticator.authenticate(getRequestWithStreamBodyAndWrongV2Signature))(validationResult => validationResult shouldBe false)
  }

  private def clientContext(test: ClientPublicKeyProvider => Any): Unit = {
    val client: ClientPublicKeyProvider = mock[ClientPublicKeyProvider]
    (client.getPublicKey _).expects(EXISTING_CLIENT_APP_UUID).returns(Future(Some(MAuthKeysHelper.getPublicKeyFromString(PUBLIC_KEY))))
    test(client)
    ()
  }
}
