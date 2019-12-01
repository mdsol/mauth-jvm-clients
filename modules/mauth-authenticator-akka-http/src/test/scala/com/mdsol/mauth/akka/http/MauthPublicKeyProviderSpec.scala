package com.mdsol.mauth.akka.http

import java.net.URI
import java.security.Security

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.mdsol.mauth.test.utils.{FakeMAuthServer, PortFinder}
import com.mdsol.mauth.{AuthenticatorConfiguration, MAuthRequest, MAuthRequestSigner, SignedRequest, UnsignedRequest}
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.scalamock.scalatest.MockFactory
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, FlatSpec, Matchers}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Right

class MauthPublicKeyProviderSpec
    extends FlatSpec
    with BeforeAndAfterAll
    with BeforeAndAfterEach
    with ScalaFutures
    with IntegrationPatience
    with Matchers
    with MockFactory {

  implicit val system: ActorSystem = ActorSystem()
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  private val EXPECTED_TIME_HEADER_VALUE = "1444672125"
  private val EXPECTED_AUTHENTICATION_HEADER_VALUE = "MWS 92a1869e-c80d-4f06-8775-6c4ebb0758e0:lTMYNWPaG4..."
  private val MAUTH_PORT = PortFinder.findFreePort()
  private val MAUTH_BASE_URL = s"http://localhost:$MAUTH_PORT"
  private val MAUTH_URL_PATH = "/mauth/v1"
  private val SECURITY_TOKENS_PATH = "/security_tokens/%s.json"
  private val FIVE_MINUTES = 300L

  val mauthHeadersWithValue = Map(
    MAuthRequest.X_MWS_AUTHENTICATION_HEADER_NAME -> EXPECTED_AUTHENTICATION_HEADER_VALUE,
    MAuthRequest.X_MWS_TIME_HEADER_NAME -> EXPECTED_TIME_HEADER_VALUE
  )

  override def beforeAll() {
    FakeMAuthServer.start(MAUTH_PORT)
    Security.addProvider(new BouncyCastleProvider)
  }

  override def beforeEach() {
    FakeMAuthServer.resetMappings()
  }

  override def afterAll() {
    FakeMAuthServer.stop()
  }

  private def getRequestUrlPath(clientAppId: String) = String.format(MAUTH_URL_PATH + SECURITY_TOKENS_PATH, clientAppId)

  private def getMAuthConfiguration = new AuthenticatorConfiguration(MAUTH_BASE_URL, MAUTH_URL_PATH, SECURITY_TOKENS_PATH, FIVE_MINUTES)

  "MauthPublicKeyProvider" should "retrieve PublicKey from MAuth Server" in {
    FakeMAuthServer.return200()
    val mockedSigner = mock[MAuthRequestSigner]
    val unsignedRequest = UnsignedRequest("GET", URI.create(MAUTH_BASE_URL + getRequestUrlPath(FakeMAuthServer.EXISTING_CLIENT_APP_UUID.toString)))
    val mockedResponse = new Right[Throwable, SignedRequest](SignedRequest(unsignedRequest, mauthHeaders = mauthHeadersWithValue))
    (mockedSigner.signRequest _).expects(*).returns(mockedResponse)

    whenReady(new MauthPublicKeyProvider(getMAuthConfiguration, mockedSigner).getPublicKey(FakeMAuthServer.EXISTING_CLIENT_APP_UUID)) { result =>
      result.toString should not be empty
    }
  }

  it should "fail on invalid response from MAuth Server" in {
    FakeMAuthServer.return401()
    val mockedSigner = mock[MAuthRequestSigner]
    val unsignedRequest = UnsignedRequest("GET", URI.create(MAUTH_BASE_URL + getRequestUrlPath(FakeMAuthServer.NON_EXISTING_CLIENT_APP_UUID.toString)))
    val mockedResponse = new Right[Throwable, SignedRequest](SignedRequest(unsignedRequest, mauthHeaders = mauthHeadersWithValue))
    (mockedSigner.signRequest _).expects(*).returns(mockedResponse)

    whenReady(new MauthPublicKeyProvider(getMAuthConfiguration, mockedSigner).getPublicKey(FakeMAuthServer.EXISTING_CLIENT_APP_UUID)) {
      case Some(_) => fail("returned a public key, expected None")
      case None =>
    }
  }

}
