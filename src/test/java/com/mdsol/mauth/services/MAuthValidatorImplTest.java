package com.mdsol.mauth.services;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.mdsol.mauth.FakeMAuthServer;
import com.mdsol.mauth.MockEpochTime;
import com.mdsol.mauth.domain.MAuthConfiguration;
import com.mdsol.mauth.domain.MAuthRequest;
import com.mdsol.mauth.exceptions.MAuthValidationException;
import com.mdsol.mauth.utils.MAuthKeysHelper;

import org.apache.commons.io.IOUtils;
import org.apache.http.client.methods.HttpPost;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.Security;
import java.util.UUID;

public class MAuthValidatorImplTest {

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

  private static final String CLIENT_UNICODE_REQUEST_SIGNATURE =
      "uJ2HR9ntb9v+5cbx8X5y/SYuQCTGz+f9jxk2AJCR02mrW/nWspN1H9jvC6Y"
          + "AF0W4TJMCWFAXV3rHG5OPnc2aEGGvSddmfW/Bkhx09IA2dRRTQ9JHdSxrQH9BGGkAP"
          + "0gMUPdP1/WiJvRh9jcEiAUJ4gmpYzL78PCJVI2uAkZm3czmiqXvXYhQmRD0KjurRfecEwA"
          + "k3VNvSbdWgoO5BM4PTTZULhjU4clfCti6+0X93ffZQGxkjcSEtIeaz2tci/YUtsYDfbfVeqX"
          + "2M3//w0OCpcBlHYXuGh9S8I1D2DCcjvC08GMJPj8HIOte0nnsIcFr5SRdfxH+5xgW7OCdUfSsKw==";
  private static final String CLIENT_UNICODE_REQUEST_METHOD = HttpPost.METHOD_NAME;
  private static final String CLIENT_UNICODE_REQUEST_PATH = "/resource/path";
  private static final String CLIENT_UNICODE_REQUEST_BODY =
      "Message with some Unicode characters inside: ș吉ń艾ęتあù";
  private static final String CLIENT_UNICODE_X_MWS_TIME_HEADER_VALUE = "1444748974";

  private static final String MAUTH_BASE_URL = "http://localhost:9001";
  private static final String MAUTH_URL_PATH = "/mauth/v1";
  private static final String SECURITY_TOKENS_PATH = "/security_tokens/%s.json";
  private static final String RESOURCE_APP_UUID = "92a1869e-c80d-4f06-8775-6c4ebb0758e0";

  private final String PUBLIC_KEY;
  private final String PRIVATE_KEY;

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @BeforeClass
  public static void setup() {
    FakeMAuthServer.start(9001);
  }

  @AfterClass
  public static void tearDown() {
    FakeMAuthServer.stop();
  }

  public MAuthValidatorImplTest() throws IOException {
    PRIVATE_KEY =
        IOUtils.toString(MAuthRequestSigner.class.getResourceAsStream("/keys/privatekey.pem"));
    PUBLIC_KEY =
        IOUtils.toString(MAuthRequestSigner.class.getResourceAsStream("/keys/publickey.pem"));
    Security.addProvider(new BouncyCastleProvider());
  }

  public MAuthRequest getRequestWithUnicodeCharactersInBody() {
    return MAuthRequest.Builder.get().withAppId(CLIENT_APP_ID)
        .withHttpMethod(CLIENT_UNICODE_REQUEST_METHOD)
        .withMessagePayload(CLIENT_UNICODE_REQUEST_BODY.getBytes(StandardCharsets.UTF_8))
        .withRequestSignature(CLIENT_UNICODE_REQUEST_SIGNATURE)
        .withRequestTime(CLIENT_UNICODE_X_MWS_TIME_HEADER_VALUE)
        .withResourcePath(CLIENT_UNICODE_REQUEST_PATH).build();
  }

  public MAuthRequest getSimpleRequest() {
    return MAuthRequest.Builder.get().withAppId(CLIENT_APP_ID).withHttpMethod(CLIENT_REQUEST_METHOD)
        .withMessagePayload(CLIENT_REQUEST_BODY.getBytes(StandardCharsets.UTF_8))
        .withRequestSignature(CLIENT_REQUEST_SIGNATURE)
        .withRequestTime(CLIENT_X_MWS_TIME_HEADER_VALUE).withResourcePath(CLIENT_REQUEST_PATH)
        .build();
  }

  public MAuthRequest getSimpleRequestWithWrongSignature() {
    return MAuthRequest.Builder.get().withAppId(CLIENT_APP_ID).withHttpMethod(CLIENT_REQUEST_METHOD)
        .withMessagePayload((CLIENT_REQUEST_BODY + " this makes this request invalid.")
            .getBytes(StandardCharsets.UTF_8))
        .withRequestSignature(CLIENT_REQUEST_SIGNATURE)
        .withRequestTime(CLIENT_X_MWS_TIME_HEADER_VALUE).withResourcePath(CLIENT_REQUEST_PATH)
        .build();
  }

  public MAuthConfiguration getMAuthConfiguration() {
    return MAuthConfiguration.Builder.get().withAppUUID(UUID.fromString(RESOURCE_APP_UUID))
        .withMauthUrl(MAUTH_BASE_URL).withMauthRequestUrlPath(MAUTH_URL_PATH)
        .withSecurityTokensUrl(SECURITY_TOKENS_PATH).withPublicKey(PUBLIC_KEY)
        .withPrivateKey(PRIVATE_KEY).build();
  }

  @Test
  public void validatingRequestSentAfter5MinutesShouldFail() throws Exception {
    // Arrange
    MAuthValidatorImpl validator = new MAuthValidatorImpl(mock(MAuthClient.class),
        new MockEpochTime(Long.parseLong(CLIENT_UNICODE_X_MWS_TIME_HEADER_VALUE) + 600));
    MAuthRequest request = getRequestWithUnicodeCharactersInBody();

    // Act & Assert
    expectedException.expect(MAuthValidationException.class);
    expectedException.expectMessage("MAuth request validation failed because of timeout 300s");
    validator.validate(request);
  }

  @Test
  public void validatingInvalidRequestShouldFail() throws Exception {
    // Arrange
    MAuthClient client = mock(MAuthClient.class);
    when(client.getPublicKey(Mockito.eq(UUID.fromString(CLIENT_APP_ID))))
        .thenReturn(MAuthKeysHelper.getPublicKeyFromString(PUBLIC_KEY));
    MAuthValidatorImpl validator = new MAuthValidatorImpl(client,
        new MockEpochTime(Long.parseLong(CLIENT_X_MWS_TIME_HEADER_VALUE) + 3));
    MAuthRequest invalidRequest = getSimpleRequestWithWrongSignature();

    // Act
    boolean validationResult = validator.validate(invalidRequest);

    // Assert
    assertThat(validationResult, equalTo(false));
  }

  @Test
  public void validatingValidRequestShouldPass() throws Exception {
    // Arrange
    MAuthClient client = mock(MAuthClient.class);
    when(client.getPublicKey(Mockito.eq(UUID.fromString(CLIENT_APP_ID))))
        .thenReturn(MAuthKeysHelper.getPublicKeyFromString(PUBLIC_KEY));
    MAuthValidatorImpl validator = new MAuthValidatorImpl(client,
        new MockEpochTime(Long.parseLong(CLIENT_X_MWS_TIME_HEADER_VALUE) + 3));
    MAuthRequest request = getSimpleRequest();

    // Act
    boolean validationResult = validator.validate(request);

    // Assert
    assertThat(validationResult, equalTo(true));
  }

  @Test
  public void validatingValidRequestWithSpecialCharactersShouldPass() throws Exception {
    // Arrange
    MAuthClient client = mock(MAuthClient.class);
    when(client.getPublicKey(Mockito.eq(UUID.fromString(CLIENT_APP_ID))))
        .thenReturn(MAuthKeysHelper.getPublicKeyFromString(PUBLIC_KEY));
    MAuthValidatorImpl validator = new MAuthValidatorImpl(client,
        new MockEpochTime(Long.parseLong(CLIENT_UNICODE_X_MWS_TIME_HEADER_VALUE) + 3));
    MAuthRequest request = getRequestWithUnicodeCharactersInBody();

    // Act
    boolean validationResult = validator.validate(request);

    // Assert
    assertThat(validationResult, equalTo(true));
  }
}
