package com.mdsol.mauth.http4s

import cats.effect.IO
import com.mdsol.mauth.RequestAuthenticatorBaseSpec
import com.mdsol.mauth.exception.MAuthValidationException
import com.mdsol.mauth.scaladsl.utils.ClientPublicKeyProvider
import com.mdsol.mauth.test.utils.FakeMAuthServer.EXISTING_CLIENT_APP_UUID
import com.mdsol.mauth.util.{EpochTimeProvider, MAuthKeysHelper}
import munit.CatsEffectSuite
import org.bouncycastle.jce.provider.BouncyCastleProvider

import java.security.Security
import java.util.UUID
import scala.concurrent.duration._

class RequestAuthenticatorSpec extends CatsEffectSuite with RequestAuthenticatorBaseSpec {

  override def beforeAll(): Unit = {
    super.beforeAll()
    Security.addProvider(new BouncyCastleProvider)
    ()
  }

  private implicit val requestValidationTimeout: Duration = 10.seconds

  def mockEpochTimeProvider(seconds: Long): EpochTimeProvider = () => seconds

  private val client: ClientPublicKeyProvider[IO] = (appUUID: UUID) =>
    if (appUUID == EXISTING_CLIENT_APP_UUID) IO.pure(Some(MAuthKeysHelper.getPublicKeyFromString(PUBLIC_KEY)))
    else IO.none

  test("authenticate a valid request") {
    //noinspection ConvertibleToMethodValue
    val authenticator = RequestAuthenticator[IO](client, mockEpochTimeProvider(CLIENT_X_MWS_TIME_HEADER_VALUE.toLong + 3))

    authenticator.authenticate(getSimpleRequest).assert
  }

  test("authenticate a request with unicode chars in body") {
    val authenticator = RequestAuthenticator(client, mockEpochTimeProvider(CLIENT_UNICODE_X_MWS_TIME_HEADER_VALUE.toLong + 3))

    authenticator.authenticate(getRequestWithUnicodeCharactersInBody).assert
  }

  test("authenticate a request without any body") {
    val authenticator = RequestAuthenticator(client, mockEpochTimeProvider(CLIENT_NO_BODY_X_MWS_TIME_HEADER_VALUE.toLong + 3))

    authenticator.authenticate(getRequestWithoutMessageBody).assert
  }

  test("not authenticate an invalid request") {
    val authenticator = RequestAuthenticator(client, mockEpochTimeProvider(CLIENT_X_MWS_TIME_HEADER_VALUE.toLong + 3))
    val invalidRequest = getSimpleRequestWithWrongSignature
    authenticator.authenticate(invalidRequest).assertEquals(false)
  }

  test("not authenticate a request after timeout period passed") {
    val client: ClientPublicKeyProvider[IO] = _ => IO.none

    val authenticator = RequestAuthenticator(client, mockEpochTimeProvider(CLIENT_UNICODE_X_MWS_TIME_HEADER_VALUE.toLong + 600))

    authenticator.authenticate(getRequestWithUnicodeCharactersInBody).handleErrorWith {
      case e: MAuthValidationException =>
        IO.pure(assertEquals(e.getMessage, "MAuth request validation failed because of timeout 10 seconds"))
      case _ => fail("should not be here")
    }
  }

  test("authenticate a valid request with V2 headers") {
    val authenticator = RequestAuthenticator(client, mockEpochTimeProvider(CLIENT_X_MWS_TIME_HEADER_VALUE.toLong + 3))

    authenticator.authenticate(getSimpleRequestV2).assert
  }

  test("authenticate a valid request with V2 headers only if V2 only enabled") {
    val authenticator = new RequestAuthenticator(client, mockEpochTimeProvider(CLIENT_X_MWS_TIME_HEADER_VALUE.toLong + 3), true)

    authenticator.authenticate(getSimpleRequestV2).assert
  }

  test("authenticate a valid request with the both V1 and V2 headers provided") {
    val authenticator = RequestAuthenticator(client, mockEpochTimeProvider(CLIENT_X_MWS_TIME_HEADER_VALUE.toLong + 3))

    authenticator.authenticate(getRequestWithAllHeaders).assert
  }

  test("reject a request with V1 headers when V2 only is enabled") {
    val client: ClientPublicKeyProvider[IO] = _ => IO.none
    val authenticator = new RequestAuthenticator(client, mockEpochTimeProvider(CLIENT_X_MWS_TIME_HEADER_VALUE.toLong + 3), true)

    authenticator.authenticate(getSimpleRequest).attempt.handleErrorWith {
      case e: MAuthValidationException =>
        IO.pure(assertEquals(e.getMessage, "The service requires mAuth v2 authentication headers."))
      case _ => fail("should not be here")
    }
  }

  test("authenticate a valid request with binary payload") {
    val client: ClientPublicKeyProvider[IO] = (appUUID: UUID) =>
      if (appUUID == UUID.fromString(CLIENT_REQUEST_BINARY_APP_UUID)) IO.pure(Some(MAuthKeysHelper.getPublicKeyFromString(PUBLIC_KEY2)))
      else IO.none
    val authenticator = RequestAuthenticator(client, mockEpochTimeProvider(CLIENT_REQUEST_BINARY_TIME_HEADER_VALUE.toLong + 3))

    authenticator.authenticate(getRequestWithBinaryBodyV1).assert
  }

  test("authenticate a valid request with binary payload for V2") {
    val client: ClientPublicKeyProvider[IO] = (appUUID: UUID) =>
      if (appUUID == UUID.fromString(CLIENT_REQUEST_BINARY_APP_UUID)) IO.pure(Some(MAuthKeysHelper.getPublicKeyFromString(PUBLIC_KEY2)))
      else IO.none
    val authenticator = RequestAuthenticator(client, mockEpochTimeProvider(CLIENT_REQUEST_BINARY_TIME_HEADER_VALUE.toLong + 5))

    authenticator.authenticate(getRequestWithBinaryBodyV2).assert
  }

  test("validate the request with the validated V1 headers and wrong V2 signature") {
    val authenticator = RequestAuthenticator(client, mockEpochTimeProvider(CLIENT_X_MWS_TIME_HEADER_VALUE.toLong + 3))

    authenticator.authenticate(getRequestWithWrongV2Signature).assert
  }

  test("fail validating request with validated V1 headers and wrong V2 signature if V2 only is enabled") {
    val authenticator = new RequestAuthenticator(client, mockEpochTimeProvider(CLIENT_X_MWS_TIME_HEADER_VALUE.toLong + 3), true)

    authenticator.authenticate(getRequestWithWrongV2Signature).assertEquals(false)
  }

  test("When payload is inputstream it should authenticate a valid request for V1") {
    val client: ClientPublicKeyProvider[IO] = (appUUID: UUID) =>
      if (appUUID == UUID.fromString(CLIENT_REQUEST_BINARY_APP_UUID)) IO.pure(Some(MAuthKeysHelper.getPublicKeyFromString(PUBLIC_KEY2)))
      else IO.none
    val authenticator = RequestAuthenticator(client, mockEpochTimeProvider(CLIENT_REQUEST_BINARY_TIME_HEADER_VALUE.toLong + 3))

    authenticator.authenticate(getRequestWithStreamBodyV1).assert
  }

  test("authenticate a valid request for V2") {
    val client: ClientPublicKeyProvider[IO] = (appUUID: UUID) =>
      if (appUUID == UUID.fromString(CLIENT_REQUEST_BINARY_APP_UUID)) IO.pure(Some(MAuthKeysHelper.getPublicKeyFromString(PUBLIC_KEY2)))
      else IO.none
    val authenticator = RequestAuthenticator(client, mockEpochTimeProvider(CLIENT_REQUEST_BINARY_TIME_HEADER_VALUE.toLong + 5))

    authenticator.authenticate(getRequestWithStreamBodyV2).assert
  }

  test("fail validating the request with the validated V1 headers and wrong V2 signature") {
    val authenticator = RequestAuthenticator(client, mockEpochTimeProvider(CLIENT_X_MWS_TIME_HEADER_VALUE.toLong + 3))

    authenticator.authenticate(getRequestWithStreamBodyAndWrongV2Signature).assertEquals(false)
  }
}
