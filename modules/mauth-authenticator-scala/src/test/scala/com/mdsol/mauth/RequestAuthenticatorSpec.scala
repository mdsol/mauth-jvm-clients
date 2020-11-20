package com.mdsol.mauth

import java.util.UUID

import com.mdsol.mauth.exception.MAuthValidationException
import com.mdsol.mauth.test.utils.FakeMAuthServer.EXISTING_CLIENT_APP_UUID
import com.mdsol.mauth.util.MAuthKeysHelper
import com.mdsol.mauth.utils.ClientPublicKeyProvider
import org.scalamock.scalatest.MockFactory
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class RequestAuthenticatorSpec extends AnyFlatSpec with RequestAuthenticatorBaseSpec with Matchers with MockFactory {

  val mockClientPublicKeyProvider: ClientPublicKeyProvider = mock[ClientPublicKeyProvider]
  val authenticator: RequestAuthenticator = new RequestAuthenticator(mockClientPublicKeyProvider, REQUEST_VALIDATION_TIMEOUT_SECONDS, mockEpochTimeProvider)
  val authenticatorV2: RequestAuthenticator =
    new RequestAuthenticator(mockClientPublicKeyProvider, REQUEST_VALIDATION_TIMEOUT_SECONDS, mockEpochTimeProvider, true)

  behavior of "RequestAuthenticator"

  it should "validate a valid request" in {
    //noinspection ConvertibleToMethodValue
    (mockEpochTimeProvider.inSeconds _: () => Long).expects().returns(CLIENT_X_MWS_TIME_HEADER_VALUE.toLong + 3)
    (mockClientPublicKeyProvider.getPublicKey _).expects(EXISTING_CLIENT_APP_UUID).returns(MAuthKeysHelper.getPublicKeyFromString(PUBLIC_KEY))

    authenticator.authenticate(getSimpleRequest) shouldBe true
  }

  it should "validate a valid request with special characters" in {
    //noinspection ConvertibleToMethodValue
    (mockEpochTimeProvider.inSeconds _: () => Long).expects().returns(CLIENT_UNICODE_X_MWS_TIME_HEADER_VALUE.toLong + 3)
    (mockClientPublicKeyProvider.getPublicKey _).expects(EXISTING_CLIENT_APP_UUID).returns(MAuthKeysHelper.getPublicKeyFromString(PUBLIC_KEY))

    authenticator.authenticate(getRequestWithUnicodeCharactersInBody) shouldBe true
  }

  it should "validate a valid request without body" in {
    //noinspection ConvertibleToMethodValue
    (mockEpochTimeProvider.inSeconds _: () => Long).expects().returns(CLIENT_NO_BODY_X_MWS_TIME_HEADER_VALUE.toLong + 3)
    (mockClientPublicKeyProvider.getPublicKey _).expects(EXISTING_CLIENT_APP_UUID).returns(MAuthKeysHelper.getPublicKeyFromString(PUBLIC_KEY))

    authenticator.authenticate(getRequestWithoutMessageBody) shouldBe true
  }

  it should "fail validating request sent after 5 minutes" in {
    //noinspection ConvertibleToMethodValue
    (mockEpochTimeProvider.inSeconds _: () => Long).expects().returns(CLIENT_UNICODE_X_MWS_TIME_HEADER_VALUE.toLong + 600)

    val expectedException = intercept[MAuthValidationException] {
      authenticator.authenticate(getRequestWithUnicodeCharactersInBody)
    }

    expectedException.getMessage shouldBe "MAuth request validation failed because of timeout 300s"
  }

  it should "fail validating invalid request" in {
    //noinspection ConvertibleToMethodValue
    (mockEpochTimeProvider.inSeconds _: () => Long).expects().returns(CLIENT_X_MWS_TIME_HEADER_VALUE.toLong + 3)
    (mockClientPublicKeyProvider.getPublicKey _).expects(EXISTING_CLIENT_APP_UUID).returns(MAuthKeysHelper.getPublicKeyFromString(PUBLIC_KEY))

    authenticator.authenticate(getSimpleRequestWithWrongSignature) shouldBe false
  }

  it should "validate a valid request for V2" in {
    //noinspection ConvertibleToMethodValue
    (mockEpochTimeProvider.inSeconds _: () => Long).expects().returns(CLIENT_MCC_TIME_HEADER_VALUE.toLong + 3)
    (mockClientPublicKeyProvider.getPublicKey _).expects(EXISTING_CLIENT_APP_UUID).returns(MAuthKeysHelper.getPublicKeyFromString(PUBLIC_KEY))

    authenticatorV2.authenticate(getSimpleRequestV2) shouldBe true
  }

  it should "validate a valid request with the headers of V1 and V2" in {
    //noinspection ConvertibleToMethodValue
    (mockEpochTimeProvider.inSeconds _: () => Long).expects().returns(CLIENT_MCC_TIME_HEADER_VALUE.toLong + 3)
    (mockClientPublicKeyProvider.getPublicKey _).expects(EXISTING_CLIENT_APP_UUID).returns(MAuthKeysHelper.getPublicKeyFromString(PUBLIC_KEY))

    authenticator.authenticate(getRequestWithAllHeaders) shouldBe true
  }

  it should "fail validating request if disasbled V1, but V2 headers missed" in {
    //noinspection ConvertibleToMethodValue
    (mockEpochTimeProvider.inSeconds _: () => Long).expects().returns(CLIENT_X_MWS_TIME_HEADER_VALUE.toLong + 3)

    val expectedException = intercept[MAuthValidationException] {
      authenticatorV2.authenticate(getSimpleRequest)
    }

    expectedException.getMessage shouldBe "The service requires mAuth v2 authentication headers."
  }

  it should "validate a valid request with binary body for V1" in {
    //noinspection ConvertibleToMethodValue
    (mockEpochTimeProvider.inSeconds _: () => Long).expects().returns(CLIENT_REQUEST_BINARY_TIME_HEADER_VALUE.toLong + 3)
    (mockClientPublicKeyProvider.getPublicKey _)
      .expects(UUID.fromString(CLIENT_REQUEST_BINARY_APP_UUID))
      .returns(MAuthKeysHelper.getPublicKeyFromString(PUBLIC_KEY2))
    authenticator.authenticate(getRequestWithBinaryBodyV1) shouldBe true
  }

  it should "validate a valid request with binary body for V2" in {
    //noinspection ConvertibleToMethodValue
    (mockEpochTimeProvider.inSeconds _: () => Long).expects().returns(CLIENT_REQUEST_BINARY_TIME_HEADER_VALUE.toLong + 5)
    (mockClientPublicKeyProvider.getPublicKey _)
      .expects(UUID.fromString(CLIENT_REQUEST_BINARY_APP_UUID))
      .returns(MAuthKeysHelper.getPublicKeyFromString(PUBLIC_KEY2))
    authenticator.authenticate(getRequestWithBinaryBodyV2) shouldBe true
  }

  it should "validate the request with the validated V1 headers and wrong V2 signature" in {
    //noinspection ConvertibleToMethodValue
    (mockEpochTimeProvider.inSeconds _: () => Long).expects().returns(CLIENT_MCC_TIME_HEADER_VALUE.toLong + 3)
    (mockClientPublicKeyProvider.getPublicKey _).expects(EXISTING_CLIENT_APP_UUID).returns(MAuthKeysHelper.getPublicKeyFromString(PUBLIC_KEY))
    authenticator.authenticate(getRequestWithWrongV2Signature) shouldBe true
  }

  it should "fail validating request with validated V1 headers and wrong V2 signature if V2 only is enabled" in {
    //noinspection ConvertibleToMethodValue
    (mockEpochTimeProvider.inSeconds _: () => Long).expects().returns(CLIENT_MCC_TIME_HEADER_VALUE.toLong + 3)
    (mockClientPublicKeyProvider.getPublicKey _).expects(EXISTING_CLIENT_APP_UUID).returns(MAuthKeysHelper.getPublicKeyFromString(PUBLIC_KEY))
    authenticatorV2.authenticate(getRequestWithWrongV2Signature) shouldBe false
  }

  "When payload is inputstream" should "authenticate a valid request for V1" in {
    //noinspection ConvertibleToMethodValue
    (mockEpochTimeProvider.inSeconds _: () => Long).expects().returns(CLIENT_REQUEST_BINARY_TIME_HEADER_VALUE.toLong + 3)
    (mockClientPublicKeyProvider.getPublicKey _)
      .expects(UUID.fromString(CLIENT_REQUEST_BINARY_APP_UUID))
      .returns(MAuthKeysHelper.getPublicKeyFromString(PUBLIC_KEY2))
    authenticator.authenticate(getRequestWithStreamBodyV1) shouldBe true
  }

  it should "authenticate a valid request for V2" in {
    //noinspection ConvertibleToMethodValue
    (mockEpochTimeProvider.inSeconds _: () => Long).expects().returns(CLIENT_REQUEST_BINARY_TIME_HEADER_VALUE.toLong + 5)
    (mockClientPublicKeyProvider.getPublicKey _)
      .expects(UUID.fromString(CLIENT_REQUEST_BINARY_APP_UUID))
      .returns(MAuthKeysHelper.getPublicKeyFromString(PUBLIC_KEY2))
    authenticator.authenticate(getRequestWithStreamBodyV2) shouldBe true
  }

  it should "fail validating request with validated V1 headers and wrong V2 signature" in {
    //noinspection ConvertibleToMethodValue
    (mockEpochTimeProvider.inSeconds _: () => Long).expects().returns(CLIENT_MCC_TIME_HEADER_VALUE.toLong + 3)
    (mockClientPublicKeyProvider.getPublicKey _).expects(EXISTING_CLIENT_APP_UUID).returns(MAuthKeysHelper.getPublicKeyFromString(PUBLIC_KEY))
    authenticator.authenticate(getRequestWithStreamBodyAndWrongV2Signature) shouldBe false
  }

}
