package com.mdsol.mauth.apache;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.mdsol.mauth.AuthenticatorConfiguration;
import com.mdsol.mauth.Signer;
import com.mdsol.mauth.exception.HttpClientPublicKeyProviderException;
import com.mdsol.mauth.test.utils.FakeMAuthServer;
import com.mdsol.mauth.utils.ClientPublicKeyProvider;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.*;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;

import java.security.Security;
import java.util.HashMap;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.mdsol.mauth.MAuthRequest.X_MWS_AUTHENTICATION_HEADER_NAME;
import static com.mdsol.mauth.MAuthRequest.X_MWS_TIME_HEADER_NAME;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class HttpClientPublicKeyProviderTest {
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private static final String EXPECTED_TIME_HEADER_VALUE = "1444672125";
  private static final String EXPECTED_AUTHENTICATION_HEADER_VALUE = "MWS 92a1869e-c80d-4f06-8775-6c4ebb0758e0:lTMYNWPaG4...";

  private static final String MAUTH_BASE_URL = "http://localhost:9001";
  private static final String MAUTH_URL_PATH = "/mauth/v1";
  private static final String SECURITY_TOKENS_PATH = "/security_tokens/%s.json";

  @BeforeClass
  public static void setup() {
    FakeMAuthServer.start(9001);
    Security.addProvider(new BouncyCastleProvider());
  }

  @Before
  public void beforeEach(){
    FakeMAuthServer.resetMappings();
  }

  @AfterClass
  public static void tearDown() {
    FakeMAuthServer.stop();
  }

  private HttpClientPublicKeyProvider getClientWithMockedSigner() {
    AuthenticatorConfiguration configuration = getMAuthConfiguration();

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

  private AuthenticatorConfiguration getMAuthConfiguration() {
    return new AuthenticatorConfiguration(MAUTH_BASE_URL, MAUTH_URL_PATH ,SECURITY_TOKENS_PATH, 300L);
  }

  @Test
  public void shouldSendCorrectRequestForPublicKeyToMAuthServer() throws Exception {
    FakeMAuthServer.return200();
    ClientPublicKeyProvider client = getClientWithMockedSigner();

    client.getPublicKey(FakeMAuthServer.EXISTING_CLIENT_APP_UUID);

    WireMock.verify(getRequestedFor(
        WireMock.urlEqualTo(getRequestUrlPath(FakeMAuthServer.EXISTING_CLIENT_APP_UUID.toString())))
        .withHeader(X_MWS_TIME_HEADER_NAME.toLowerCase(),
            WireMock.equalTo(EXPECTED_TIME_HEADER_VALUE))
        .withHeader(X_MWS_AUTHENTICATION_HEADER_NAME.toLowerCase(),
            WireMock.equalTo(EXPECTED_AUTHENTICATION_HEADER_VALUE)));
  }

  @Test
  public void shouldThrowHttpClientPublicKeyProviderExceptionOnInvalidResponseCode() throws Exception {
    FakeMAuthServer.return401();
    ClientPublicKeyProvider client = getClientWithMockedSigner();

    expectedException.expect(HttpClientPublicKeyProviderException.class);
    expectedException.expectMessage("Invalid response code returned by server: 401");

    client.getPublicKey(FakeMAuthServer.NON_EXISTING_CLIENT_APP_UUID);
  }
}