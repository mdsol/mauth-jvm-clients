package com.mdsol.mauth.akka.http

import java.net.URI
import java.security.{PublicKey, Security}

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.mdsol.mauth.test.utils.FakeMAuthServer
import com.mdsol.mauth.{AuthenticatorConfiguration, MAuthRequestSigner, SignedRequest, UnsignedRequest}
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.mockito.Matchers.any
import org.mockito.Mockito.{mock, when}
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, FlatSpec, Matchers}

import scala.util.Right

class MauthPublicKeyProviderTest extends FlatSpec with BeforeAndAfterAll with BeforeAndAfterEach with ScalaFutures with IntegrationPatience with Matchers {

  implicit val system: ActorSystem = ActorSystem()
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  private val EXPECTED_TIME_HEADER_VALUE = "1444672125"
  private val EXPECTED_AUTHENTICATION_HEADER_VALUE = "MWS 92a1869e-c80d-4f06-8775-6c4ebb0758e0:lTMYNWPaG4..."
  private val MAUTH_PORT = 9001
  private val MAUTH_BASE_URL = s"http://localhost:$MAUTH_PORT"
  private val MAUTH_URL_PATH = "/mauth/v1"
  private val SECURITY_TOKENS_PATH = "/security_tokens/%s.json"

  override def beforeAll(): Unit = {
    FakeMAuthServer.start(MAUTH_PORT)
    Security.addProvider(new BouncyCastleProvider)
  }

  override def beforeEach(): Unit = {
    FakeMAuthServer.resetMappings()
  }

  override def afterAll(): Unit = {
    FakeMAuthServer.stop()
  }

  private def getRequestUrlPath(clientAppId: String) = String.format(MAUTH_URL_PATH + SECURITY_TOKENS_PATH, clientAppId)

  private def getMAuthConfiguration = new AuthenticatorConfiguration(MAUTH_BASE_URL, MAUTH_URL_PATH, SECURITY_TOKENS_PATH, 300L)

  "MauthPublicKeyProvider" should "retrieve PublicKey from MAuth Server" in {
    FakeMAuthServer.return200()
    val mockedSigner = mock(classOf[MAuthRequestSigner])
    val unsignedRequest = UnsignedRequest("GET", URI.create(MAUTH_BASE_URL + getRequestUrlPath(FakeMAuthServer.EXISTING_CLIENT_APP_UUID.toString)))
    val mockedResponse = new Right[Throwable, SignedRequest](SignedRequest(unsignedRequest, EXPECTED_AUTHENTICATION_HEADER_VALUE, EXPECTED_TIME_HEADER_VALUE))
    when(mockedSigner.signRequest(any(classOf[UnsignedRequest]))).thenReturn(mockedResponse)

    whenReady(new MauthPublicKeyProvider(getMAuthConfiguration, mockedSigner).getPublicKey(FakeMAuthServer.EXISTING_CLIENT_APP_UUID)) { result: Option[PublicKey] =>
      result.toString should not be empty
    }
  }

  it should "fail on invalid response from MAuth Server" in {
    FakeMAuthServer.return401()
    val mockedSigner = mock(classOf[MAuthRequestSigner])
    val unsignedRequest = UnsignedRequest("GET", URI.create(MAUTH_BASE_URL + getRequestUrlPath(FakeMAuthServer.NON_EXISTING_CLIENT_APP_UUID.toString)))
    val mockedResponse = new Right[Throwable, SignedRequest](SignedRequest(unsignedRequest, EXPECTED_AUTHENTICATION_HEADER_VALUE, EXPECTED_TIME_HEADER_VALUE))
    when(mockedSigner.signRequest(any(classOf[UnsignedRequest]))).thenReturn(mockedResponse)

    whenReady(new MauthPublicKeyProvider(getMAuthConfiguration, mockedSigner).getPublicKey(FakeMAuthServer.EXISTING_CLIENT_APP_UUID)) {
      case Some(_) => fail("returned a public key, expected None")
      case None =>
    }
  }

}