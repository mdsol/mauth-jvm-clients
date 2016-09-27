package com.mdsol.mauth.utils;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.mdsol.mauth.MAuthConfiguration;
import com.mdsol.mauth.Signer;
import com.mdsol.mauth.exception.HttpClientPublicKeyProviderException;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;

import java.security.Security;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class HttpClientPublicKeyProviderTest {
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private static final String X_MWS_TIME_HEADER_NAME = "x-mws-time";
  private static final String EXPECTED_TIME_HEADER_VALUE = "1444672125";
  private static final String X_MWS_AUTHENTICATION_HEADER_NAME = "x-mws-authentication";
  private static final String EXPECTED_AUTHENTICATION_HEADER_VALUE = "MWS 92a1869e-c80d-4f06-8775-6c4ebb0758e0:lTMYNWPaG4...";

  private static final String MAUTH_BASE_URL = "http://localhost:9001";
  private static final String MAUTH_URL_PATH = "/mauth/v1";
  private static final String SECURITY_TOKENS_PATH = "/security_tokens/%s.json";
  private static final UUID RESOURCE_APP_UUID = UUID.fromString("92a1869e-c80d-4f06-8775-6c4ebb0758e0");

  //private static final String PUBLIC_KEY = FixturesLoader.getPublicKey();
  private static final String PRIVATE_KEY = FixturesLoader.getPrivateKey();

  @BeforeClass
  public static void setup() {
    FakeMAuthServer.start(9001);
    Security.addProvider(new BouncyCastleProvider());
  }

  @AfterClass
  public static void tearDown() {
    FakeMAuthServer.stop();
  }

  private HttpClientPublicKeyProvider getClientWithMockedSigner() {
    MAuthConfiguration configuration = getMAuthConfiguration();

    Signer mockedSigner = mock(Signer.class);
    Map<String, String> mockedHeaders = new HashMap<>();
    mockedHeaders.put(X_MWS_AUTHENTICATION_HEADER_NAME, EXPECTED_AUTHENTICATION_HEADER_VALUE);
    mockedHeaders.put(X_MWS_TIME_HEADER_NAME, EXPECTED_TIME_HEADER_VALUE);
    when(mockedSigner.generateRequestHeaders(eq("GET"), Mockito.anyString(), eq("")))
        .thenReturn(mockedHeaders);

    return new HttpClientPublicKeyProvider(configuration, mockedSigner);
  }

  private String getRequestUrlPath(String clientAppId) {
    return String.format(MAUTH_URL_PATH + SECURITY_TOKENS_PATH, clientAppId);
  }

  private MAuthConfiguration getMAuthConfiguration() {
    return new MAuthConfiguration(RESOURCE_APP_UUID, MAUTH_BASE_URL, PRIVATE_KEY, MAUTH_URL_PATH ,SECURITY_TOKENS_PATH);
  }

  @Test
  public void shouldSendCorrectRequestForPublicKeyToMAuthServer() throws Exception {
    // Arrange
    FakeMAuthServer.return200();
    ClientPublicKeyProvider client = getClientWithMockedSigner();

    // Act
    client.getPublicKey(UUID.fromString(FakeMAuthServer.EXISTING_CLIENT_APP_UUID));

    // Assert
    WireMock.verify(getRequestedFor(
        WireMock.urlEqualTo(getRequestUrlPath(FakeMAuthServer.EXISTING_CLIENT_APP_UUID)))
        .withHeader(X_MWS_TIME_HEADER_NAME.toLowerCase(),
            WireMock.equalTo(EXPECTED_TIME_HEADER_VALUE))
        .withHeader(X_MWS_AUTHENTICATION_HEADER_NAME.toLowerCase(),
            WireMock.equalTo(EXPECTED_AUTHENTICATION_HEADER_VALUE)));
  }

  @Test
  public void shouldThrowHttpClientPublicKeyProviderExceptionOnInvalidResponseCode() throws Exception {
    // Arrange
    FakeMAuthServer.return401();
    ClientPublicKeyProvider client = getClientWithMockedSigner();

    // Assert & Act
    expectedException.expect(HttpClientPublicKeyProviderException.class);
    expectedException.expectMessage("Invalid response code returned by server: 401");

    client.getPublicKey(UUID.fromString(FakeMAuthServer.NON_EXISTING_CLIENT_APP_UUID));
  }
}