package com.mdsol.mauth.internals.client;

import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.mdsol.mauth.domain.MAuthConfiguration;
import com.mdsol.mauth.exceptions.MAuthHttpClientException;
import com.mdsol.mauth.internals.signer.MAuthSignerImpl;
import com.mdsol.mauth.utils.FakeMAuthServer;
import com.mdsol.mauth.utils.FixturesLoader;

import com.github.tomakehurst.wiremock.client.WireMock;

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

public class MAuthHttpClientTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private static final String X_MWS_TIME_HEADER_NAME = "X-Mws-Time";
  private static final String EXPECTED_TIME_HEADER_VALUE = "1444672125";
  private static final String X_MWS_AUTHENTICATION_HEADER_NAME = "X-Mws-Authentication";
  private static final String EXPECTED_AUTHENTICATION_HEADER_VALUE =
      "MWS 92a1869e-c80d-4f06-8775-6c4ebb0758e0:lTMYNWPaG4...";

  private static final String MAUTH_BASE_URL = "http://localhost:9001";
  private static final String MAUTH_URL_PATH = "/mauth/v1";
  private static final String SECURITY_TOKENS_PATH = "/security_tokens/%s.json";
  private static final UUID RESOURCE_APP_UUID =
      UUID.fromString("92a1869e-c80d-4f06-8775-6c4ebb0758e0");

  private final String PUBLIC_KEY = FixturesLoader.getPublicKey();
  private final String PRIVATE_KEY = FixturesLoader.getPrivateKey();

  @BeforeClass
  public static void setup() {
    FakeMAuthServer.start(9001);
    Security.addProvider(new BouncyCastleProvider());
  }

  @AfterClass
  public static void tearDown() {
    FakeMAuthServer.stop();
  }

  private MAuthHttpClient getClientWithMockedSigner() throws Exception {
    MAuthConfiguration configuration = getMAuthConfiguration();

    MAuthSignerImpl mockedSigner = mock(MAuthSignerImpl.class);
    Map<String, String> mockedHeaders = new HashMap<>();
    mockedHeaders.put(X_MWS_AUTHENTICATION_HEADER_NAME, EXPECTED_AUTHENTICATION_HEADER_VALUE);
    mockedHeaders.put(X_MWS_TIME_HEADER_NAME, EXPECTED_TIME_HEADER_VALUE);
    when(mockedSigner.generateRequestHeaders(eq("GET"), Mockito.anyString(), eq("")))
        .thenReturn(mockedHeaders);

    return new MAuthHttpClient(configuration, mockedSigner);
  }

  private String getRequestUrlPath(String clientAppId) {
    return String.format(MAUTH_URL_PATH + SECURITY_TOKENS_PATH, clientAppId);
  }

  private MAuthConfiguration getMAuthConfiguration() {
    return MAuthConfiguration.Builder.get().withAppUUID(RESOURCE_APP_UUID)
        .withMauthUrl(MAUTH_BASE_URL).withMauthRequestUrlPath(MAUTH_URL_PATH)
        .withSecurityTokensUrl(SECURITY_TOKENS_PATH).withPublicKey(PUBLIC_KEY)
        .withPrivateKey(PRIVATE_KEY).build();
  }

  @Test
  public void shouldSendCorrectRequestForPublicKeyToMAuthServer() throws Exception {
    // Arrange
    FakeMAuthServer.return200();
    MAuthHttpClient client = getClientWithMockedSigner();

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
  public void shouldThrowMAuthHttpClientExceptionOnInvalidResponseCode() throws Exception {
    // Arrange
    FakeMAuthServer.return401();
    MAuthHttpClient client = getClientWithMockedSigner();

    // Assert & Act
    expectedException.expect(MAuthHttpClientException.class);
    expectedException.expectMessage("Invalid response code returned by server: 401");

    client.getPublicKey(UUID.fromString(FakeMAuthServer.NON_EXISTING_CLIENT_APP_UUID));
  }

}
