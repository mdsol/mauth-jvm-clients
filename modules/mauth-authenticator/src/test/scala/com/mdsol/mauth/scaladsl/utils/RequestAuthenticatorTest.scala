package com.mdsol.mauth.scaladsl.utils

import java.security.Security

import com.mdsol.mauth.RequestAuthenticatorTestBase
import com.mdsol.mauth.exception.MAuthValidationException
import com.mdsol.mauth.test.utils.FakeMAuthServer.EXISTING_CLIENT_APP_UUID
import com.mdsol.mauth.util.MAuthKeysHelper
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.mockito.Matchers.{eq => eqTo}
import org.mockito.Mockito.{mock, when}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FlatSpec, Matchers}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class RequestAuthenticatorTest extends FlatSpec with RequestAuthenticatorTestBase with Matchers with ScalaFutures {

  Security.addProvider(new BouncyCastleProvider)

  "RequestAuthenticator" should "authenticate a valid request" in {
    val client = mock(classOf[ClientPublicKeyProvider])
    when(client.getPublicKey(eqTo(EXISTING_CLIENT_APP_UUID))).thenReturn(Future(Some(MAuthKeysHelper.getPublicKeyFromString(RequestAuthenticatorTestBase.PUBLIC_KEY))))
    when(RequestAuthenticatorTestBase.mockEpochTimeProvider.inSeconds).thenReturn(RequestAuthenticatorTestBase.CLIENT_X_MWS_TIME_HEADER_VALUE.toLong + 3)
    val authenticator = new RequestAuthenticator(client, RequestAuthenticatorTestBase.REQUEST_VALIDATION_TIMEOUT_SECONDS, RequestAuthenticatorTestBase.mockEpochTimeProvider)

    whenReady(authenticator.authenticate(getSimpleRequest)) { validationResult =>
      validationResult shouldBe true
    }
  }

  it should "authenticate a request with unicode chars in body" in {
    val client = mock(classOf[ClientPublicKeyProvider])
    when(client.getPublicKey(eqTo(EXISTING_CLIENT_APP_UUID))).thenReturn(Future(Some(MAuthKeysHelper.getPublicKeyFromString(RequestAuthenticatorTestBase.PUBLIC_KEY))))
    when(RequestAuthenticatorTestBase.mockEpochTimeProvider.inSeconds).thenReturn(RequestAuthenticatorTestBase.CLIENT_UNICODE_X_MWS_TIME_HEADER_VALUE.toLong + 3)
    val authenticator = new RequestAuthenticator(client, RequestAuthenticatorTestBase.REQUEST_VALIDATION_TIMEOUT_SECONDS, RequestAuthenticatorTestBase.mockEpochTimeProvider)

    whenReady(authenticator.authenticate(getRequestWithUnicodeCharactersInBody)) { validationResult =>
      validationResult shouldBe true
    }
  }

  it should "authenticate a request without any body" in {
    val client = mock(classOf[ClientPublicKeyProvider])
    when(client.getPublicKey(eqTo(EXISTING_CLIENT_APP_UUID))).thenReturn(Future(Some(MAuthKeysHelper.getPublicKeyFromString(RequestAuthenticatorTestBase.PUBLIC_KEY))))
    when(RequestAuthenticatorTestBase.mockEpochTimeProvider.inSeconds).thenReturn(RequestAuthenticatorTestBase.CLIENT_NO_BODY_X_MWS_TIME_HEADER_VALUE.toLong + 3)
    val authenticator = new RequestAuthenticator(client, RequestAuthenticatorTestBase.REQUEST_VALIDATION_TIMEOUT_SECONDS, RequestAuthenticatorTestBase.mockEpochTimeProvider)

    whenReady(authenticator.authenticate(getRequestWithoutMessageBody)) { validationResult =>
      validationResult shouldBe true
    }
  }

  it should "not authenticate an invalid request" in {
    val client = mock(classOf[ClientPublicKeyProvider])
    when(client.getPublicKey(eqTo(EXISTING_CLIENT_APP_UUID))).thenReturn(Future(Some(MAuthKeysHelper.getPublicKeyFromString(RequestAuthenticatorTestBase.PUBLIC_KEY))))
    when(RequestAuthenticatorTestBase.mockEpochTimeProvider.inSeconds).thenReturn(RequestAuthenticatorTestBase.CLIENT_X_MWS_TIME_HEADER_VALUE.toLong + 3)
    val authenticator = new RequestAuthenticator(client, RequestAuthenticatorTestBase.REQUEST_VALIDATION_TIMEOUT_SECONDS, RequestAuthenticatorTestBase.mockEpochTimeProvider)
    val invalidRequest = getSimpleRequestWithWrongSignature

    whenReady(authenticator.authenticate(invalidRequest)) { validationResult =>
      validationResult shouldBe false
    }
  }

  it should "not authenticate a request after timeout period passed" in {
    when(RequestAuthenticatorTestBase.mockEpochTimeProvider.inSeconds).thenReturn(RequestAuthenticatorTestBase.CLIENT_UNICODE_X_MWS_TIME_HEADER_VALUE.toLong + 600)
    val authenticator = new RequestAuthenticator(mock(classOf[ClientPublicKeyProvider]), RequestAuthenticatorTestBase.REQUEST_VALIDATION_TIMEOUT_SECONDS, RequestAuthenticatorTestBase.mockEpochTimeProvider)

    whenReady(authenticator.authenticate(getRequestWithUnicodeCharactersInBody).failed) {
      case e: MAuthValidationException => e.getMessage shouldBe "MAuth request validation failed because of timeout 300s"
      case _ => fail("should not be here")
    }
  }
}
