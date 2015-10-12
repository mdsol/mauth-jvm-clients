package com.mdsol.mauth;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.isEmptyOrNullString;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.apache.commons.io.IOUtils;
import org.bouncycastle.openssl.PEMReader;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.IOException;
import java.io.StringReader;
import java.security.PublicKey;

import javax.servlet.http.HttpServletResponse;

@RunWith(PowerMockRunner.class)
@PrepareForTest(MAuthClient.class)
public class MAuthClientTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private static final String CLIENT_APP_ID = "162a4b03-4db9-4631-9d1f-d7195f37128d";
  private static final String CLIENT_SIGNATURE =
      "fFQzIOo4S1MxxmEDB9v7v0IYNytnS3I5aHNeJfEfFe1v1gTE/cH36BfLG"
          + "/zpOHX7kPUDUnmVZK1MuqdenLDmY6q2h8BR/1yA7n/dupQfnVZIeaB99GIO"
          + "FJaHT6P6gXHiMTFxgX3Oo/rj97jfDUxaunxnlqnfhHccPxbhiqfcVgHahw6hiX"
          + "x9sAt/iG/Yu7lzZbWx4SWkDyAGvk1I5rpbxcOWLPYad8qpuCFaHPMQJTTze8G02hd5"
          + "ocZDzQS73MOfUqPLfvv7aLRvBS078cqs2uAip84n8bM4qG60gMfoC9kUluza7i9poyFqq"
          + "IdsCnS5RQuyNcsixneX2X3CNt3yOw==";

  private static final String X_MWS_AUTHENTICATION_HEADER_NAME = "X-Mws-Authentication";
  private static final String X_MWS_AUTHENTICATION_HEADER_VALUE =
      "MWS " + CLIENT_APP_ID + ":" + CLIENT_SIGNATURE;

  private static final String X_MWS_TIME_HEADER_NAME = "X-Mws-Time";
  private static final String X_MWS_TIME_HEADER_VALUE = "1444672122";

  private static final String MAUTH_BASE_URL = "http://localhost:9001";
  private static final String MAUTH_URL_PATH = "/mauth/v1";
  private static final String SECURITY_TOKENS_PATH = "/security_tokens/%s.json";
  private static final String RESOURCE_APP_UUID = "92a1869e-c80d-4f06-8775-6c4ebb0758e0";
  private final String PUBLIC_KEY;
  private final String PRIVATE_KEY;

  public MAuthClientTest() throws IOException {
    PRIVATE_KEY = IOUtils.toString(MAuthRequestSigner.class.getResourceAsStream("privatekey.pem"));
    PUBLIC_KEY = IOUtils.toString(MAuthRequestSigner.class.getResourceAsStream("publickey.pem"));
  }

  private MAuthClient createClientUsingValidTestConfiguration() throws Exception {
    return new MAuthClient(MAUTH_BASE_URL, MAUTH_URL_PATH, SECURITY_TOKENS_PATH, RESOURCE_APP_UUID,
        PUBLIC_KEY, PRIVATE_KEY);
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
  public void validatingValidRequestShouldPass() throws Exception {
    // Arrange
    String signatureInHeader = CLIENT_SIGNATURE;
    String localTime = X_MWS_TIME_HEADER_VALUE;
    String verb = "POST";
    String resourceUrl = "/resource/path";
    String body = "message here";
    String appId = CLIENT_APP_ID;

    MAuthClient client = PowerMockito.spy(createClientUsingValidTestConfiguration());

    EpochTime remoteTime = mockEpochTime(Long.valueOf(localTime) + 3);
    client.setEpochTime(remoteTime);

    PublicKey key = generatePublicKeyFromString(PUBLIC_KEY);

    PowerMockito.doReturn(key).when(client, "getPublicKey", appId);

    // Act
    boolean validationResult =
        client.validateRequest(signatureInHeader, localTime, verb, resourceUrl, body, appId);

    // Assert
    assertThat(validationResult, equalTo(true));
  }

  private PublicKey generatePublicKeyFromString(final String publicKey) throws IOException {
    PublicKey key = null;
    try (PEMReader reader = new PEMReader(new StringReader(publicKey))) {
      key = (PublicKey) reader.readObject();
    };
    return key;
  }

  @Test
  public void getAppIdInHeaderShouldReturnCorrectAppId() throws Exception {
    // Arrange
    MAuthClient client = createClientUsingValidTestConfiguration();

    // Act
    String appId = client.getAppIdInHeader(X_MWS_AUTHENTICATION_HEADER_VALUE);

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
    String signature = client.getSignatureInHeader(X_MWS_AUTHENTICATION_HEADER_VALUE);

    // Assert
    assertThat(signature, equalTo(CLIENT_SIGNATURE));
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
