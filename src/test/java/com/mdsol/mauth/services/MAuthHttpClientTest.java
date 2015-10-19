package com.mdsol.mauth.services;

import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;

import com.mdsol.mauth.FakeMAuthServer;
import com.mdsol.mauth.MockEpochTime;
import com.mdsol.mauth.domain.MAuthConfiguration;

import com.github.tomakehurst.wiremock.client.WireMock;

import org.apache.commons.io.IOUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.util.UUID;

public class MAuthHttpClientTest {
  
  private static final String MAUTH_BASE_URL = "http://localhost:9001";
  private static final String MAUTH_URL_PATH = "/mauth/v1";
  private static final String SECURITY_TOKENS_PATH = "/security_tokens/%s.json";
  private static final String RESOURCE_APP_UUID = "92a1869e-c80d-4f06-8775-6c4ebb0758e0";
  
  private static final String X_MWS_TIME_HEADER_NAME = "X-Mws-Time";
  private static final String X_MWS_AUTHENTICATION_HEADER_NAME = "X-Mws-Authentication";
  private static final String CLIENT_X_MWS_TIME_HEADER_VALUE = "1444672122";
  private final String PUBLIC_KEY;
  private final String PRIVATE_KEY;

  public MAuthHttpClientTest() throws IOException {
    PRIVATE_KEY =
        IOUtils.toString(MAuthRequestSigner.class.getResourceAsStream("/keys/privatekey.pem"));
    PUBLIC_KEY =
        IOUtils.toString(MAuthRequestSigner.class.getResourceAsStream("/keys/publickey.pem"));
  }
  
  private MAuthHttpClient createClientUsingValidTestConfiguration() throws Exception {
    MAuthConfiguration configuration = new MAuthConfiguration(RESOURCE_APP_UUID, PUBLIC_KEY, PRIVATE_KEY, MAUTH_BASE_URL, MAUTH_URL_PATH, SECURITY_TOKENS_PATH);
    return new MAuthHttpClient(configuration, new MAuthRequestSigner(UUID.fromString(RESOURCE_APP_UUID), PRIVATE_KEY)); // mock(MAuthRequestSigner.class));
  }

  @BeforeClass
  public static void setup() {
    FakeMAuthServer.start(9001);
  }

  @AfterClass
  public static void tearDown() {
    FakeMAuthServer.stop();
  }
  
  @Test
  public void validatingValidRequestShouldSendCorrectRequestForPublicKeyToMauthServer()
      throws Exception {
    // Arrange
    FakeMAuthServer.return200();
    MAuthHttpClient client = createClientUsingValidTestConfiguration();
    MAuthRequestSigner.setEpochTime(new MockEpochTime(Long.valueOf(CLIENT_X_MWS_TIME_HEADER_VALUE) + 3));

    // Act
    client.getPublicKey(FakeMAuthServer.EXISTING_CLIENT_APP_UUID);

    // Assert
    WireMock.verify(getRequestedFor(
        WireMock.urlEqualTo(String.format(MAUTH_URL_PATH + SECURITY_TOKENS_PATH, FakeMAuthServer.EXISTING_CLIENT_APP_UUID)))
            .withHeader(X_MWS_TIME_HEADER_NAME.toLowerCase(), WireMock.equalTo("1444672125"))
            .withHeader(X_MWS_AUTHENTICATION_HEADER_NAME.toLowerCase(), WireMock.equalTo(
                "MWS 92a1869e-c80d-4f06-8775-6c4ebb0758e0:lTMYNWPaG42sN26tzopSrlGgPrc7xmwIZ6"
                    + "eMu7v/nOM/F4JnKIadUmnaijnh0T2rbhx1m1Nr5qGNT/Q8xZDb4kkCLsxAeYn/12NRFtMwDzE80Z"
                    + "sLWaTkEOl8rv1PHDV/B8abvkwuOiqq5MQ7fmRcw80oA6lRtwBkIRGIGT9a48CJSoV28n4jwNHxPpKL"
                    + "ao8qmHvq2PzrJJyx9FqJII28ii1vvNmlQ4I0ZJQHCXEvdZkVnJ2tA8jo88nEJ8roGcRwUtX9qkIE6SpW"
                    + "C2knHI5nj8GV12XaBMXZC0pLrBFykwJirTXMGLFlajniWSs+vCI0xPSrZ99Xj7xPNH5I+D2SJg==")));
  }
  
}
