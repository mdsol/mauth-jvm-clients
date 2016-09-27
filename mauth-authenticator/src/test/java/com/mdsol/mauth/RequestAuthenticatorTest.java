package com.mdsol.mauth;

import com.mdsol.mauth.exception.MAuthValidationException;
import com.mdsol.mauth.test.utils.FixturesLoader;
import com.mdsol.mauth.util.EpochTimeProvider;
import com.mdsol.mauth.util.MAuthKeysHelper;
import com.mdsol.mauth.utils.ClientPublicKeyProvider;
import com.mdsol.mauth.utils.FakeMAuthServer;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;

import java.nio.charset.StandardCharsets;
import java.security.Security;
import java.util.UUID;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class RequestAuthenticatorTest {
  private static final String CLIENT_APP_ID = FakeMAuthServer.EXISTING_CLIENT_APP_UUID;
  private static final String CLIENT_REQUEST_SIGNATURE =
      "fFQzIOo4S1MxxmEDB9v7v0IYNytnS3I5aHNeJfEfFe1v1gTE/cH36BfLG"
          + "/zpOHX7kPUDUnmVZK1MuqdenLDmY6q2h8BR/1yA7n/dupQfnVZIeaB99GIO"
          + "FJaHT6P6gXHiMTFxgX3Oo/rj97jfDUxaunxnlqnfhHccPxbhiqfcVgHahw6hiX"
          + "x9sAt/iG/Yu7lzZbWx4SWkDyAGvk1I5rpbxcOWLPYad8qpuCFaHPMQJTTze8G02hd5"
          + "ocZDzQS73MOfUqPLfvv7aLRvBS078cqs2uAip84n8bM4qG60gMfoC9kUluza7i9poyFqq"
          + "IdsCnS5RQuyNcsixneX2X3CNt3yOw==";
  private static final String CLIENT_REQUEST_AUTHENTICATION_HEADER =
      "MWS " + CLIENT_APP_ID + ":" + CLIENT_REQUEST_SIGNATURE;
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
  private static final String CLIENT_UNICODE_REQUEST_AUTHENTICATION_HEADER =
      "MWS " + CLIENT_APP_ID + ":" + CLIENT_UNICODE_REQUEST_SIGNATURE;
  private static final String CLIENT_UNICODE_REQUEST_METHOD = HttpPost.METHOD_NAME;
  private static final String CLIENT_UNICODE_REQUEST_PATH = "/resource/path";
  private static final String CLIENT_UNICODE_REQUEST_BODY =
      "Message with some Unicode characters inside: ș吉ń艾ęتあù";
  private static final String CLIENT_UNICODE_X_MWS_TIME_HEADER_VALUE = "1444748974";

  private static final String CLIENT_NO_BODY_REQUEST_SIGNATURE =
      "NddGBdXnB3/ne3oCmYJQ20mASPHifsI0sG3mt034jjRfjlTafOYJ/kt3RJYk"
          + "OMLT104GtzTgFfQBeTSJpOrBK/+EK9T0V+JNmjrU6Y9FpcH4p3hB2liooKjHK"
          + "fs0L1u3wEG5VOK5xzpjTxO4SQeFQ7GhoAJpNh1p3kcJIPrxRUy3Fbi3FZzeWfOevS9yrj"
          + "idU3713xNsg1d/nJP63b/2zT+mcaZHaDHhQ6IL2z9bKc7H7sBqMSJaqJ4GpuNZPvAd/lkP9/n"
          + "25w5Jd5fbA+phj+K3MIJWmIETItzS9pt5YgAWW1PjAuZd3w9ugTOXwfWNbc7YIAeCqMRMVp5NLndzww==";
  private static final String CLIENT_NO_BODY_REQUEST_AUTHENTICATION_HEADER =
      "MWS " + CLIENT_APP_ID + ":" + CLIENT_NO_BODY_REQUEST_SIGNATURE;
  private static final String CLIENT_NO_BODY_REQUEST_METHOD = HttpGet.METHOD_NAME;
  private static final String CLIENT_NO_BODY_REQUEST_PATH = "/resource/path";
  private static final String CLIENT_NO_BODY_X_MWS_TIME_HEADER_VALUE = "1424700000";

  private static final String PUBLIC_KEY = FixturesLoader.getPublicKey();
  public static final long REQUEST_VALIDATION_TIMEOUT_SECONDS = 300L;
  private EpochTimeProvider mockEpochTimeProvider = mock(EpochTimeProvider.class);

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @BeforeClass
  public static void setup() {
    FakeMAuthServer.start(9001);
    Security.addProvider(new BouncyCastleProvider());
  }

  @AfterClass
  public static void tearDown() {
    FakeMAuthServer.stop();
  }

  @Test
  public void validatingRequestSentAfter5MinutesShouldFail() throws Exception {
    when(mockEpochTimeProvider.inSeconds()).thenReturn(Long.parseLong(CLIENT_UNICODE_X_MWS_TIME_HEADER_VALUE) + 600);
    RequestAuthenticator authenticator = new RequestAuthenticator(mock(ClientPublicKeyProvider.class), REQUEST_VALIDATION_TIMEOUT_SECONDS, mockEpochTimeProvider);
    MAuthRequest request = getRequestWithUnicodeCharactersInBody();

    expectedException.expect(MAuthValidationException.class);
    expectedException.expectMessage("MAuth request validation failed because of timeout 300s");
    authenticator.authenticate(request);
  }

  @Test
  public void validatingInvalidRequestShouldFail() throws Exception {
    ClientPublicKeyProvider client = mock(ClientPublicKeyProvider.class);
    when(client.getPublicKey(Mockito.eq(UUID.fromString(CLIENT_APP_ID)))).thenReturn(MAuthKeysHelper.getPublicKeyFromString(PUBLIC_KEY));
    when(mockEpochTimeProvider.inSeconds()).thenReturn(Long.parseLong(CLIENT_X_MWS_TIME_HEADER_VALUE) + 3);
    Authenticator authenticator = new RequestAuthenticator(client, REQUEST_VALIDATION_TIMEOUT_SECONDS, mockEpochTimeProvider);
    MAuthRequest invalidRequest = getSimpleRequestWithWrongSignature();

    boolean validationResult = authenticator.authenticate(invalidRequest);

    assertThat(validationResult, equalTo(false));
  }

  @Test
  public void validatingValidRequestShouldPass() throws Exception {
    ClientPublicKeyProvider client = mock(ClientPublicKeyProvider.class);
    when(client.getPublicKey(Mockito.eq(UUID.fromString(CLIENT_APP_ID)))).thenReturn(MAuthKeysHelper.getPublicKeyFromString(PUBLIC_KEY));
    when(mockEpochTimeProvider.inSeconds()).thenReturn(Long.parseLong(CLIENT_X_MWS_TIME_HEADER_VALUE) + 3);
    Authenticator authenticator = new RequestAuthenticator(client, REQUEST_VALIDATION_TIMEOUT_SECONDS, mockEpochTimeProvider);
    MAuthRequest request = getSimpleRequest();

    boolean validationResult = authenticator.authenticate(request);

    assertThat(validationResult, equalTo(true));
  }

  @Test
  public void validatingValidRequestWithSpecialCharactersShouldPass() throws Exception {
    ClientPublicKeyProvider client = mock(ClientPublicKeyProvider.class);
    when(client.getPublicKey(Mockito.eq(UUID.fromString(CLIENT_APP_ID)))).thenReturn(MAuthKeysHelper.getPublicKeyFromString(PUBLIC_KEY));
    when(mockEpochTimeProvider.inSeconds()).thenReturn(Long.parseLong(CLIENT_UNICODE_X_MWS_TIME_HEADER_VALUE) + 3);
    Authenticator authenticator = new RequestAuthenticator(client, REQUEST_VALIDATION_TIMEOUT_SECONDS, mockEpochTimeProvider);
    MAuthRequest request = getRequestWithUnicodeCharactersInBody();

    boolean validationResult = authenticator.authenticate(request);

    assertThat(validationResult, equalTo(true));
  }

  @Test
  public void validatingValidRequestWithoutBodyShouldPass() throws Exception {
    ClientPublicKeyProvider client = mock(ClientPublicKeyProvider.class);
    when(client.getPublicKey(Mockito.eq(UUID.fromString(CLIENT_APP_ID)))).thenReturn(MAuthKeysHelper.getPublicKeyFromString(PUBLIC_KEY));
    when(mockEpochTimeProvider.inSeconds()).thenReturn(Long.parseLong(CLIENT_NO_BODY_X_MWS_TIME_HEADER_VALUE) + 3);
    Authenticator authenticator = new RequestAuthenticator(client, REQUEST_VALIDATION_TIMEOUT_SECONDS, mockEpochTimeProvider);
    MAuthRequest request = getRequestWithoutMessageBody();

    boolean validationResult = authenticator.authenticate(request);

    assertThat(validationResult, equalTo(true));
  }

  private MAuthRequest getSimpleRequest() {
    return MAuthRequest.Builder.get()
        .withAuthenticationHeaderValue(CLIENT_REQUEST_AUTHENTICATION_HEADER)
        .withTimeHeaderValue(CLIENT_X_MWS_TIME_HEADER_VALUE).withHttpMethod(CLIENT_REQUEST_METHOD)
        .withMessagePayload(CLIENT_REQUEST_BODY.getBytes(StandardCharsets.UTF_8))
        .withResourcePath(CLIENT_REQUEST_PATH).build();
  }

  private MAuthRequest getSimpleRequestWithWrongSignature() {
    return MAuthRequest.Builder.get()
        .withAuthenticationHeaderValue(CLIENT_REQUEST_AUTHENTICATION_HEADER)
        .withTimeHeaderValue(CLIENT_X_MWS_TIME_HEADER_VALUE).withHttpMethod(CLIENT_REQUEST_METHOD)
        .withMessagePayload((CLIENT_REQUEST_BODY + " this makes this request invalid.")
            .getBytes(StandardCharsets.UTF_8))
        .withResourcePath(CLIENT_REQUEST_PATH).build();
  }

  private MAuthRequest getRequestWithUnicodeCharactersInBody() {
    return MAuthRequest.Builder.get()
        .withAuthenticationHeaderValue(CLIENT_UNICODE_REQUEST_AUTHENTICATION_HEADER)
        .withTimeHeaderValue(CLIENT_UNICODE_X_MWS_TIME_HEADER_VALUE)
        .withHttpMethod(CLIENT_UNICODE_REQUEST_METHOD)
        .withMessagePayload(CLIENT_UNICODE_REQUEST_BODY.getBytes(StandardCharsets.UTF_8))
        .withResourcePath(CLIENT_UNICODE_REQUEST_PATH).build();
  }

  private MAuthRequest getRequestWithoutMessageBody() {
    return MAuthRequest.Builder.get()
        .withAuthenticationHeaderValue(CLIENT_NO_BODY_REQUEST_AUTHENTICATION_HEADER)
        .withTimeHeaderValue(CLIENT_NO_BODY_X_MWS_TIME_HEADER_VALUE)
        .withHttpMethod(CLIENT_NO_BODY_REQUEST_METHOD).withResourcePath(CLIENT_NO_BODY_REQUEST_PATH)
        .build();
  }
}