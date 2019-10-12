package com.mdsol.mauth.scaladsl.utils

import com.mdsol.mauth.RequestAuthenticatorBaseSpec
import com.mdsol.mauth.exception.MAuthValidationException
import com.mdsol.mauth.scaladsl.RequestAuthenticator
import com.mdsol.mauth.test.utils.FakeMAuthServer.EXISTING_CLIENT_APP_UUID
import com.mdsol.mauth.util.MAuthKeysHelper
import org.scalamock.scalatest.MockFactory
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FlatSpec, Matchers}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.language.postfixOps

class RequestAuthenticatorSpec extends FlatSpec with RequestAuthenticatorBaseSpec with Matchers with ScalaFutures with MockFactory {

  private implicit val requestValidationTimeout: Duration = 10 seconds

  behavior of "RequestAuthenticator"

  it should "authenticate a valid request" in clientContext { (client) =>
    //noinspection ConvertibleToMethodValue
    (mockEpochTimeProvider.inSeconds _: () => Long).expects().returns(CLIENT_X_MWS_TIME_HEADER_VALUE.toLong + 3)
    val authenticator = new RequestAuthenticator(client, mockEpochTimeProvider)

    whenReady(authenticator.authenticate(getSimpleRequest)) { validationResult =>
      validationResult shouldBe true
    }
  }

  it should "authenticate a request with unicode chars in body" in clientContext { (client) =>
    //noinspection ConvertibleToMethodValue
    (mockEpochTimeProvider.inSeconds _: () => Long).expects().returns(CLIENT_UNICODE_X_MWS_TIME_HEADER_VALUE.toLong + 3)
    val authenticator = new RequestAuthenticator(client, mockEpochTimeProvider)

    whenReady(authenticator.authenticate(getRequestWithUnicodeCharactersInBody)) { validationResult =>
      validationResult shouldBe true
    }
  }

  it should "authenticate a request without any body" in clientContext { (client) =>
    //noinspection ConvertibleToMethodValue
    (mockEpochTimeProvider.inSeconds _: () => Long).expects().returns(CLIENT_NO_BODY_X_MWS_TIME_HEADER_VALUE.toLong + 3)
    val authenticator = new RequestAuthenticator(client, mockEpochTimeProvider)

    whenReady(authenticator.authenticate(getRequestWithoutMessageBody)) { validationResult =>
      validationResult shouldBe true
    }
  }

  it should "not authenticate an invalid request" in clientContext { (client) =>
    //noinspection ConvertibleToMethodValue
    (mockEpochTimeProvider.inSeconds _: () => Long).expects().returns(CLIENT_X_MWS_TIME_HEADER_VALUE.toLong + 3)
    val authenticator = new RequestAuthenticator(client, mockEpochTimeProvider)
    val invalidRequest = getSimpleRequestWithWrongSignature

    whenReady(authenticator.authenticate(invalidRequest)) { validationResult =>
      validationResult shouldBe false
    }
  }

  it should "not authenticate a request after timeout period passed" in {
    //noinspection ConvertibleToMethodValue
    (mockEpochTimeProvider.inSeconds _: () => Long).expects().returns(CLIENT_UNICODE_X_MWS_TIME_HEADER_VALUE.toLong + 600)
    val authenticator = new RequestAuthenticator(mock[ClientPublicKeyProvider], mockEpochTimeProvider)

    whenReady(authenticator.authenticate(getRequestWithUnicodeCharactersInBody).failed) {
      case e: MAuthValidationException => e.getMessage shouldBe "MAuth request validation failed because of timeout 10 seconds"
      case _ => fail("should not be here")
    }
  }

  private def clientContext(test: (ClientPublicKeyProvider) => Any): Unit = {
    val client: ClientPublicKeyProvider = mock[ClientPublicKeyProvider]
    (client.getPublicKey _).expects(EXISTING_CLIENT_APP_UUID).returns(Future(Some(MAuthKeysHelper.getPublicKeyFromString(PUBLIC_KEY))))
    test(client)
  }
}
