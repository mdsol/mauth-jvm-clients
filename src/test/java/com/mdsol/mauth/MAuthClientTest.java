package com.mdsol.mauth;

import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.isEmptyOrNullString;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.tomakehurst.wiremock.client.WireMock;

import org.apache.commons.io.IOUtils;
import org.apache.http.client.methods.HttpPost;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.ArgumentCaptor;

import java.io.IOException;

import javax.servlet.http.HttpServletResponse;

public class MAuthClientTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private static final String X_MWS_TIME_HEADER_NAME = "X-Mws-Time";
  private static final String X_MWS_AUTHENTICATION_HEADER_NAME = "X-Mws-Authentication";

  private static final String CLIENT_APP_ID = FakeMAuthServer.EXISTING_CLIENT_APP_UUID;

  private static final String CLIENT_REQUEST_SIGNATURE =
      "fFQzIOo4S1MxxmEDB9v7v0IYNytnS3I5aHNeJfEfFe1v1gTE/cH36BfLG"
          + "/zpOHX7kPUDUnmVZK1MuqdenLDmY6q2h8BR/1yA7n/dupQfnVZIeaB99GIO"
          + "FJaHT6P6gXHiMTFxgX3Oo/rj97jfDUxaunxnlqnfhHccPxbhiqfcVgHahw6hiX"
          + "x9sAt/iG/Yu7lzZbWx4SWkDyAGvk1I5rpbxcOWLPYad8qpuCFaHPMQJTTze8G02hd5"
          + "ocZDzQS73MOfUqPLfvv7aLRvBS078cqs2uAip84n8bM4qG60gMfoC9kUluza7i9poyFqq"
          + "IdsCnS5RQuyNcsixneX2X3CNt3yOw==";
  private static final String CLIENT_REQUEST_METHOD = HttpPost.METHOD_NAME;
  private static final String CLIENT_REQUEST_PATH = "/resource/path";
  private static final String CLIENT_REQUEST_BODY = "message here";
  private static final String CLIENT_X_MWS_TIME_HEADER_VALUE = "1444672122";
  private static final String CLIENT_X_MWS_AUTHENTICATION_HEADER_VALUE =
      "MWS " + CLIENT_APP_ID + ":" + CLIENT_REQUEST_SIGNATURE;

  private static final String CLIENT_UNICODE_REQUEST_SIGNATURE =
      "uJ2HR9ntb9v+5cbx8X5y/SYuQCTGz+f9jxk2AJCR02mrW/nWspN1H9jvC6Y"
          + "AF0W4TJMCWFAXV3rHG5OPnc2aEGGvSddmfW/Bkhx09IA2dRRTQ9JHdSxrQH9BGGkAP"
          + "0gMUPdP1/WiJvRh9jcEiAUJ4gmpYzL78PCJVI2uAkZm3czmiqXvXYhQmRD0KjurRfecEwA"
          + "k3VNvSbdWgoO5BM4PTTZULhjU4clfCti6+0X93ffZQGxkjcSEtIeaz2tci/YUtsYDfbfVeqX"
          + "2M3//w0OCpcBlHYXuGh9S8I1D2DCcjvC08GMJPj8HIOte0nnsIcFr5SRdfxH+5xgW7OCdUfSsKw==";
  private static final String CLIENT_UNICODE_REQUEST_METHOD = HttpPost.METHOD_NAME;
  private static final String CLIENT_UNICODE_REQUEST_PATH = "/resource/path";
  private static final String CLIENT_UNICODE_REQUEST_BODY = "Message with some Unicode characters inside: ș吉ń艾ęتあù";
  private static final String CLIENT_UNICODE_X_MWS_TIME_HEADER_VALUE = "1444748974";

  private static final String MAUTH_BASE_URL = "http://localhost:9001";
  private static final String MAUTH_URL_PATH = "/mauth/v1";
  private static final String SECURITY_TOKENS_PATH = "/security_tokens/%s.json";
  private static final String RESOURCE_APP_UUID = "92a1869e-c80d-4f06-8775-6c4ebb0758e0";

  private final String PUBLIC_KEY;
  private final String PRIVATE_KEY;

  public MAuthClientTest() throws IOException {
    PRIVATE_KEY =
        IOUtils.toString(MAuthRequestSigner.class.getResourceAsStream("/keys/privatekey.pem"));
    PUBLIC_KEY =
        IOUtils.toString(MAuthRequestSigner.class.getResourceAsStream("/keys/publickey.pem"));
  }

  private MAuthClient createClientUsingValidTestConfiguration() throws Exception {
    return new MAuthClient(MAUTH_BASE_URL, MAUTH_URL_PATH, SECURITY_TOKENS_PATH, RESOURCE_APP_UUID,
        PUBLIC_KEY, PRIVATE_KEY);
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
  public void getSignedResponseShouldReturnResponseWithHeaderContainingStatusBodyAndAppId()
      throws Exception {
    // Arrange
    MAuthClient client = createClientUsingValidTestConfiguration();
    EpochTime epochTime = mockEpochTime(0L);
    client.setEpochTime(epochTime);
    HttpServletResponse response = mock(HttpServletResponse.class);

    // Act
    client.getSignedResponse(response);

    // Assert
    ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
    verify(response).addHeader(argThat(equalTo(X_MWS_AUTHENTICATION_HEADER_NAME.toLowerCase())),
        captor.capture());
    assertThat(captor.getValue(),
        equalTo("cl7ISRVypveTy2QnckV+yCsefGbM8jlCsmeEjDDvpgx1V5gTO9RNHe5LT+y1BXdaahXv9G"
            + "PgjdSDCMsK6y8VfKVyzxaEmZJ6UvewRfatlkHk5MttBMWmcvCy1sben8xzDTaMFfg1gLJVc6n"
            + "pzp6cHNpNhFyetC1FHrhSIFMWuAWi9APZbgj8IZ/Wpa/Bnz7oR1LRlWlC7TvWwy6CGNwRE/4pT"
            + "w0QVtflGnjYK/5hDuQ2ayu7B7eRVEK/Q/IbBU0OerpJ+UhhuJIAsb98ztr3yZPHNSD/TerqKICP"
            + "665sQTiQuBDDxwXv3/lGUX3hT3FWRU8iIMifZr6qUp6vdevbYQ=="));

    verify(response).addHeader(argThat(equalTo(X_MWS_TIME_HEADER_NAME.toLowerCase())),
        argThat(not(isEmptyOrNullString())));
  }

  private EpochTime mockEpochTime(long seconds) {
    EpochTime epochTime = mock(EpochTime.class);
    when(epochTime.getSeconds()).thenReturn(seconds);
    return epochTime;
  }

  @Test
  public void validatingValidRequestShouldSendCorrectRequestForPublicKeyToMauthServer()
      throws Exception {
    // Arrange
    FakeMAuthServer.return200();
    MAuthClient client = createClientUsingValidTestConfiguration();
    EpochTime remoteTime = mockEpochTime(Long.valueOf(CLIENT_X_MWS_TIME_HEADER_VALUE) + 3);
    client.setEpochTime(remoteTime);

    // Act
    client.validateRequest(CLIENT_REQUEST_SIGNATURE, CLIENT_X_MWS_TIME_HEADER_VALUE,
        CLIENT_REQUEST_METHOD, CLIENT_REQUEST_PATH, CLIENT_REQUEST_BODY, CLIENT_APP_ID);

    // Assert
    WireMock.verify(getRequestedFor(
        WireMock.urlEqualTo(String.format(MAUTH_URL_PATH + SECURITY_TOKENS_PATH, CLIENT_APP_ID)))
            .withHeader(X_MWS_TIME_HEADER_NAME.toLowerCase(), WireMock.equalTo("1444672125"))
            .withHeader(X_MWS_AUTHENTICATION_HEADER_NAME.toLowerCase(), WireMock.equalTo(
                "MWS 92a1869e-c80d-4f06-8775-6c4ebb0758e0:lTMYNWPaG42sN26tzopSrlGgPrc7xmwIZ6"
                    + "eMu7v/nOM/F4JnKIadUmnaijnh0T2rbhx1m1Nr5qGNT/Q8xZDb4kkCLsxAeYn/12NRFtMwDzE80Z"
                    + "sLWaTkEOl8rv1PHDV/B8abvkwuOiqq5MQ7fmRcw80oA6lRtwBkIRGIGT9a48CJSoV28n4jwNHxPpKL"
                    + "ao8qmHvq2PzrJJyx9FqJII28ii1vvNmlQ4I0ZJQHCXEvdZkVnJ2tA8jo88nEJ8roGcRwUtX9qkIE6SpW"
                    + "C2knHI5nj8GV12XaBMXZC0pLrBFykwJirTXMGLFlajniWSs+vCI0xPSrZ99Xj7xPNH5I+D2SJg==")));
  }

  @Test
  public void validatingValidRequestShouldPass() throws Exception {
    // Arrange
    FakeMAuthServer.return200();
    MAuthClient client = createClientUsingValidTestConfiguration();
    EpochTime remoteTime = mockEpochTime(Long.valueOf(CLIENT_X_MWS_TIME_HEADER_VALUE) + 3);
    client.setEpochTime(remoteTime);

    // Act
    boolean validationResult =
        client.validateRequest(CLIENT_REQUEST_SIGNATURE, CLIENT_X_MWS_TIME_HEADER_VALUE,
            CLIENT_REQUEST_METHOD, CLIENT_REQUEST_PATH, CLIENT_REQUEST_BODY, CLIENT_APP_ID);

    // Assert
    assertThat(validationResult, equalTo(true));
  }

  @Test
  public void validatingValidRequestWithSpecialCharactersShouldPass() throws Exception {
    // Arrange
    FakeMAuthServer.return200();
    MAuthClient client = createClientUsingValidTestConfiguration();
    EpochTime remoteTime = mockEpochTime(Long.valueOf(CLIENT_X_MWS_TIME_HEADER_VALUE) + 3);
    client.setEpochTime(remoteTime);

    // Act
    boolean validationResult = client.validateRequest(CLIENT_UNICODE_REQUEST_SIGNATURE,
        CLIENT_UNICODE_X_MWS_TIME_HEADER_VALUE, CLIENT_UNICODE_REQUEST_METHOD,
        CLIENT_UNICODE_REQUEST_PATH, CLIENT_UNICODE_REQUEST_BODY, CLIENT_APP_ID);

    // Assert
    assertThat(validationResult, equalTo(true));
  }

  @Test
  public void validatingRequestSentAfter5MinutesShouldFail() throws Exception {
    // Arrange
    FakeMAuthServer.return200();
    MAuthClient client = createClientUsingValidTestConfiguration();
    EpochTime remoteTime = mockEpochTime(Long.valueOf(CLIENT_X_MWS_TIME_HEADER_VALUE) + 600);
    client.setEpochTime(remoteTime);

    // Act & Assert
    expectedException.expect(Exception.class);
    expectedException.expectMessage("validateRequest error: epoc time is older than 5 minutes");

    client.validateRequest(CLIENT_REQUEST_SIGNATURE, CLIENT_X_MWS_TIME_HEADER_VALUE,
        CLIENT_REQUEST_METHOD, CLIENT_REQUEST_PATH, CLIENT_REQUEST_BODY, CLIENT_APP_ID);
  }

  @Test
  public void getAppIdInHeaderShouldReturnCorrectAppId() throws Exception {
    // Arrange
    MAuthClient client = createClientUsingValidTestConfiguration();

    // Act
    String appId = client.getAppIdInHeader(CLIENT_X_MWS_AUTHENTICATION_HEADER_VALUE);

    // Assert
    assertThat(appId, equalTo(CLIENT_APP_ID));
  }

  @Test
  public void getAppIdInHeaderShouldReturnEmptyStringIfHeaderValueIsInvalid() throws Exception {
    // Arrange
    MAuthClient client = createClientUsingValidTestConfiguration();

    // Act
    String appId = client.getAppIdInHeader("Invalid header here!");

    // Assert
    assertThat(appId, equalTo(""));
  }

  @Test
  public void getSignatureInHeaderShouldReturnCorrectSignature() throws Exception {
    // Arrange
    MAuthClient client = createClientUsingValidTestConfiguration();

    // Act
    String signature = client.getSignatureInHeader(CLIENT_X_MWS_AUTHENTICATION_HEADER_VALUE);

    // Assert
    assertThat(signature, equalTo(CLIENT_REQUEST_SIGNATURE));
  }

  @Test
  public void getSignatureInHeaderShouldReturnEmptyStringIfHeaderValueIsInvalid() throws Exception {
    // Arrange
    MAuthClient client = createClientUsingValidTestConfiguration();

    // Act
    String signature = client.getSignatureInHeader("Invalid header here!");

    // Assert
    assertThat(signature, equalTo(""));
  }

  @Test
  public void shouldThrowExceptionOnNullBaseUrl() throws Exception {
    // Arrange
    expectedException.expect(Exception.class);
    expectedException.expectMessage("Cannot initialize MAuth client: mAuthUrl cannot be null");

    // Act
    new MAuthClient(null, MAUTH_URL_PATH, SECURITY_TOKENS_PATH, RESOURCE_APP_UUID, PUBLIC_KEY,
        PRIVATE_KEY);
  }

  @Test
  public void shouldThrowExceptionOnEmptyBaseUrl() throws Exception {
    // Arrange
    expectedException.expect(Exception.class);
    expectedException.expectMessage("Cannot initialize MAuth client: mAuthUrl cannot be null");

    // Act
    new MAuthClient("", MAUTH_URL_PATH, SECURITY_TOKENS_PATH, RESOURCE_APP_UUID, PUBLIC_KEY,
        PRIVATE_KEY);
  }

  @Test
  public void shouldThrowExceptionOnNullApiPath() throws Exception {
    // Arrange
    expectedException.expect(Exception.class);
    expectedException
        .expectMessage("Cannot initialize MAuth client: mAuthRequestUrlPath cannot be null");

    // Act
    new MAuthClient(MAUTH_BASE_URL, null, SECURITY_TOKENS_PATH, RESOURCE_APP_UUID, PUBLIC_KEY,
        PRIVATE_KEY);
  }

  @Test
  public void shouldThrowExceptionOnEmptyApiPath() throws Exception {
    // Arrange
    expectedException.expect(Exception.class);
    expectedException
        .expectMessage("Cannot initialize MAuth client: mAuthRequestUrlPath cannot be null");

    // Act
    new MAuthClient(MAUTH_BASE_URL, "", SECURITY_TOKENS_PATH, RESOURCE_APP_UUID, PUBLIC_KEY,
        PRIVATE_KEY);
  }

  @Test
  public void shouldThrowExceptionOnNullSecurityTokensPath() throws Exception {
    // Arrange
    expectedException.expect(Exception.class);
    expectedException
        .expectMessage("Cannot initialize MAuth client: securityTokensUrl cannot be null");

    // Act
    new MAuthClient(MAUTH_BASE_URL, MAUTH_URL_PATH, null, RESOURCE_APP_UUID, PUBLIC_KEY,
        PRIVATE_KEY);
  }

  @Test
  public void shouldThrowExceptionOnEmptySecurityTokensPath() throws Exception {
    // Arrange
    expectedException.expect(Exception.class);
    expectedException
        .expectMessage("Cannot initialize MAuth client: securityTokensUrl cannot be null");

    // Act
    new MAuthClient(MAUTH_BASE_URL, MAUTH_URL_PATH, "", RESOURCE_APP_UUID, PUBLIC_KEY, PRIVATE_KEY);
  }

  @Test
  public void shouldThrowExceptionOnNullAppId() throws Exception {
    // Arrange
    expectedException.expect(Exception.class);
    expectedException.expectMessage("Cannot initialize MAuth client: appId cannot be null");

    // Act
    new MAuthClient(MAUTH_BASE_URL, MAUTH_URL_PATH, SECURITY_TOKENS_PATH, null, PUBLIC_KEY,
        PRIVATE_KEY);
  }

  @Test
  public void shouldThrowExceptionOnEmptyAppId() throws Exception {
    // Arrange
    expectedException.expect(Exception.class);
    expectedException.expectMessage("Cannot initialize MAuth client: appId cannot be null");

    // Act
    new MAuthClient(MAUTH_BASE_URL, MAUTH_URL_PATH, SECURITY_TOKENS_PATH, "", PUBLIC_KEY,
        PRIVATE_KEY);
  }

  @Test
  public void shouldThrowExceptionOnNullPublicKey() throws Exception {
    // Arrange
    expectedException.expect(Exception.class);
    expectedException.expectMessage("Cannot initialize MAuth client: publicKey cannot be null");

    // Act
    new MAuthClient(MAUTH_BASE_URL, MAUTH_URL_PATH, SECURITY_TOKENS_PATH, RESOURCE_APP_UUID, null,
        PRIVATE_KEY);
  }

  @Test
  public void shouldThrowExceptionOnEmptyPublicKey() throws Exception {
    // Arrange
    expectedException.expect(Exception.class);
    expectedException.expectMessage("Cannot initialize MAuth client: publicKey cannot be null");

    // Act
    new MAuthClient(MAUTH_BASE_URL, MAUTH_URL_PATH, SECURITY_TOKENS_PATH, RESOURCE_APP_UUID, "",
        PRIVATE_KEY);
  }

  @Test
  public void shouldThrowExceptionOnNullPrivateKey() throws Exception {
    // Arrange
    expectedException.expect(Exception.class);
    expectedException.expectMessage("Cannot initialize MAuth client: privateKey cannot be null");

    // Act
    new MAuthClient(MAUTH_BASE_URL, MAUTH_URL_PATH, SECURITY_TOKENS_PATH, RESOURCE_APP_UUID,
        PUBLIC_KEY, null);
  }

  @Test
  public void shouldThrowExceptionOnEmptyPrivateKey() throws Exception {
    // Arrange
    expectedException.expect(Exception.class);
    expectedException.expectMessage("Cannot initialize MAuth client: privateKey cannot be null");

    // Act
    new MAuthClient(MAUTH_BASE_URL, MAUTH_URL_PATH, SECURITY_TOKENS_PATH, RESOURCE_APP_UUID,
        PUBLIC_KEY, "");
  }

}
