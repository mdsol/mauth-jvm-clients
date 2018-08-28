package com.mdsol.mauth

import com.mdsol.mauth.exception.MAuthValidationException
import com.mdsol.mauth.test.utils.FakeMAuthServer.EXISTING_CLIENT_APP_UUID
import com.mdsol.mauth.util.MAuthKeysHelper
import com.mdsol.mauth.utils.ClientPublicKeyProvider
import org.scalamock.scalatest.MockFactory
import org.scalatest.{FlatSpec, Matchers}

class RequestAuthenticatorSpec extends FlatSpec with RequestAuthenticatorBaseSpec with Matchers with MockFactory {

  val mockClientPublicKeyProvider: ClientPublicKeyProvider = mock[ClientPublicKeyProvider]
  val authenticator: RequestAuthenticator = new RequestAuthenticator(mockClientPublicKeyProvider, REQUEST_VALIDATION_TIMEOUT_SECONDS, mockEpochTimeProvider)

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
}
