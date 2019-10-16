package com.mdsol.mauth.apache

import java.security.Security
import java.util

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor
import com.mdsol.mauth.MAuthRequest.{MCC_AUTHENTICATION_HEADER_NAME, MCC_TIME_HEADER_NAME, X_MWS_AUTHENTICATION_HEADER_NAME, X_MWS_TIME_HEADER_NAME}
import com.mdsol.mauth.exception.HttpClientPublicKeyProviderException
import com.mdsol.mauth.test.utils.{FakeMAuthServer, PortFinder}
import com.mdsol.mauth.utils.ClientPublicKeyProvider
import com.mdsol.mauth.{AuthenticatorConfiguration, Signer}
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.scalamock.scalatest.MockFactory
import org.scalatest._

class HttpClientPublicKeyProviderSpec extends FlatSpec with Matchers with MockFactory with BeforeAndAfterAll with BeforeAndAfterEach {
  private val EXPECTED_TIME_HEADER_VALUE: String = "1444672125"
  private val EXPECTED_AUTHENTICATION_HEADER_VALUE: String = "MWS 92a1869e-c80d-4f06-8775-6c4ebb0758e0:lTMYNWPaG4...CXQ=="
  private val EXPECTED_AUTHENTICATION_HEADER_VALUE_V2: String = "MWSV2 92a1869e-c80d-4f06-8775-6c4ebb0758e0:F3xY8X...AOg==;"
  private val port = PortFinder.findFreePort()
  private val MAUTH_BASE_URL: String = s"http://localhost:$port"
  private val MAUTH_URL_PATH: String = "/mauth/v1"
  private val SECURITY_TOKENS_PATH: String = "/security_tokens/%s.json"
  private val timeToLive = 300L

  override protected def beforeAll() {
    FakeMAuthServer.start(port)
    Security.addProvider(new BouncyCastleProvider)
  }

  override protected def beforeEach() {
    FakeMAuthServer.resetMappings()
  }

  override protected def afterAll() {
    FakeMAuthServer.stop()
  }

  private def getClientWithMockedSigner: HttpClientPublicKeyProvider = {
    val configuration: AuthenticatorConfiguration = getMAuthConfiguration
    val mockedSigner: Signer = mock[Signer]
    val mockedHeaders: util.Map[String, String] = new util.HashMap[String, String]
    mockedHeaders.put(X_MWS_AUTHENTICATION_HEADER_NAME, EXPECTED_AUTHENTICATION_HEADER_VALUE)
    mockedHeaders.put(X_MWS_TIME_HEADER_NAME, EXPECTED_TIME_HEADER_VALUE)
    mockedHeaders.put(MCC_AUTHENTICATION_HEADER_NAME, EXPECTED_AUTHENTICATION_HEADER_VALUE_V2)
    mockedHeaders.put(MCC_TIME_HEADER_NAME, EXPECTED_TIME_HEADER_VALUE)
    (mockedSigner.generateRequestHeadersV2 _).expects("GET", *, "", "").returns(mockedHeaders)
    new HttpClientPublicKeyProvider(configuration, mockedSigner)
  }

  private def getRequestUrlPath(clientAppId: String): String = String.format(MAUTH_URL_PATH + SECURITY_TOKENS_PATH, clientAppId)

  private def getMAuthConfiguration: AuthenticatorConfiguration =
    new AuthenticatorConfiguration(MAUTH_BASE_URL, MAUTH_URL_PATH, SECURITY_TOKENS_PATH, timeToLive, false)

  it should "send correct request for public key to MAuth server" in {
    FakeMAuthServer.return200()
    val client: ClientPublicKeyProvider = getClientWithMockedSigner
    client.getPublicKey(FakeMAuthServer.EXISTING_CLIENT_APP_UUID)
    WireMock.verify(
      getRequestedFor(WireMock.urlEqualTo(getRequestUrlPath(FakeMAuthServer.EXISTING_CLIENT_APP_UUID.toString)))
        .withHeader(X_MWS_TIME_HEADER_NAME.toLowerCase, WireMock.equalTo(EXPECTED_TIME_HEADER_VALUE))
        .withHeader(X_MWS_AUTHENTICATION_HEADER_NAME.toLowerCase, WireMock.equalTo(EXPECTED_AUTHENTICATION_HEADER_VALUE))
    )
  }

  it should "throw HttpClientPublicKeyProviderException on invalid response code" in {
    FakeMAuthServer.return401()
    val client: ClientPublicKeyProvider = getClientWithMockedSigner
    val expectedException = intercept[HttpClientPublicKeyProviderException](client.getPublicKey(FakeMAuthServer.NON_EXISTING_CLIENT_APP_UUID))
    expectedException.getMessage should include("Invalid response code returned by server: 401")
  }
}
