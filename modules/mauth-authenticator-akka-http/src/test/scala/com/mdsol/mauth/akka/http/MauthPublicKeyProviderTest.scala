package com.mdsol.mauth.akka.http

import java.net.URI
import java.security.PublicKey
import java.util.UUID

import com.mdsol.mauth.test.utils.FakeMAuthServer
import com.mdsol.mauth.{AuthenticatorConfiguration, MAuthRequestSigner, SignedRequest, UnsignedRequest}
import org.mockito.Matchers.any
import org.mockito.Mockito.{mock, when}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, FlatSpec, Matchers}

import scala.util.Right

class MauthPublicKeyProviderTest extends FlatSpec with BeforeAndAfterAll with BeforeAndAfterEach with ScalaFutures with Matchers {
  private val X_MWS_TIME_HEADER_NAME = "x-mws-time"
  private val EXPECTED_TIME_HEADER_VALUE = "1444672125"
  private val X_MWS_AUTHENTICATION_HEADER_NAME = "x-mws-authentication"
  private val EXPECTED_AUTHENTICATION_HEADER_VALUE = "MWS 92a1869e-c80d-4f06-8775-6c4ebb0758e0:lTMYNWPaG4..."

  private val MAUTH_BASE_URL = "http://localhost:9001"
  private val MAUTH_URL_PATH = "/mauth/v1"
  private val SECURITY_TOKENS_PATH = "/security_tokens/%s.json"

  override def beforeAll(): Unit = {
    FakeMAuthServer.start(9001)
  }

  override def beforeEach(): Unit = {
    FakeMAuthServer.resetMappings()
  }

  private def getRequestUrlPath(clientAppId: String) = String.format(MAUTH_URL_PATH + SECURITY_TOKENS_PATH, clientAppId)

  private def getMAuthConfiguration = new AuthenticatorConfiguration(MAUTH_BASE_URL, MAUTH_URL_PATH, SECURITY_TOKENS_PATH, 300L)

  "MauthPublicKeyProvider" should "retrieve PublicKey from MAuth Server" in {
    val mockedSigner = mock(classOf[MAuthRequestSigner])
    val unsignedRequest = UnsignedRequest("GET", URI.create("http://google.com"))
    val mockedResponse = new Right[Throwable, SignedRequest](SignedRequest(unsignedRequest, EXPECTED_AUTHENTICATION_HEADER_VALUE, EXPECTED_TIME_HEADER_VALUE))
    when(mockedSigner.signRequest(any(classOf[UnsignedRequest]))).thenReturn(mockedResponse)

    val publicKeyProvider: MauthPublicKeyProvider = new MauthPublicKeyProvider(getMAuthConfiguration, mockedSigner)
    whenReady(publicKeyProvider.getPublicKey(UUID.fromString(FakeMAuthServer.EXISTING_CLIENT_APP_UUID))){ result: PublicKey =>
      result.toString should not be empty
    }
  }


}