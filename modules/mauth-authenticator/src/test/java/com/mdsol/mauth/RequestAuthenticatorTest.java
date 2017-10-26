package com.mdsol.mauth;

import com.mdsol.mauth.exception.MAuthValidationException;
import com.mdsol.mauth.util.MAuthKeysHelper;
import com.mdsol.mauth.utils.ClientPublicKeyProvider;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;

import java.security.Security;

import static com.mdsol.mauth.test.utils.FakeMAuthServer.EXISTING_CLIENT_APP_UUID;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class RequestAuthenticatorTest implements RequestAuthenticatorTestBase {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @BeforeClass
  public static void setup() {
    Security.addProvider(new BouncyCastleProvider());
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
    when(client.getPublicKey(Mockito.eq(EXISTING_CLIENT_APP_UUID))).thenReturn(MAuthKeysHelper.getPublicKeyFromString(PUBLIC_KEY));
    when(mockEpochTimeProvider.inSeconds()).thenReturn(Long.parseLong(CLIENT_X_MWS_TIME_HEADER_VALUE) + 3);
    Authenticator authenticator = new RequestAuthenticator(client, REQUEST_VALIDATION_TIMEOUT_SECONDS, mockEpochTimeProvider);
    MAuthRequest invalidRequest = getSimpleRequestWithWrongSignature();

    boolean validationResult = authenticator.authenticate(invalidRequest);

    assertThat(validationResult, equalTo(false));
  }

  @Test
  public void validatingValidRequestShouldPass() throws Exception {
    ClientPublicKeyProvider client = mock(ClientPublicKeyProvider.class);
    when(client.getPublicKey(Mockito.eq(EXISTING_CLIENT_APP_UUID))).thenReturn(MAuthKeysHelper.getPublicKeyFromString(PUBLIC_KEY));
    when(mockEpochTimeProvider.inSeconds()).thenReturn(Long.parseLong(CLIENT_X_MWS_TIME_HEADER_VALUE) + 3);
    Authenticator authenticator = new RequestAuthenticator(client, REQUEST_VALIDATION_TIMEOUT_SECONDS, mockEpochTimeProvider);
    MAuthRequest request = getSimpleRequest();

    boolean validationResult = authenticator.authenticate(request);

    assertThat(validationResult, equalTo(true));
  }

  @Test
  public void validatingValidRequestWithSpecialCharactersShouldPass() throws Exception {
    ClientPublicKeyProvider client = mock(ClientPublicKeyProvider.class);
    when(client.getPublicKey(Mockito.eq(EXISTING_CLIENT_APP_UUID))).thenReturn(MAuthKeysHelper.getPublicKeyFromString(PUBLIC_KEY));
    when(mockEpochTimeProvider.inSeconds()).thenReturn(Long.parseLong(CLIENT_UNICODE_X_MWS_TIME_HEADER_VALUE) + 3);
    Authenticator authenticator = new RequestAuthenticator(client, REQUEST_VALIDATION_TIMEOUT_SECONDS, mockEpochTimeProvider);
    MAuthRequest request = getRequestWithUnicodeCharactersInBody();

    boolean validationResult = authenticator.authenticate(request);

    assertThat(validationResult, equalTo(true));
  }

  @Test
  public void validatingValidRequestWithoutBodyShouldPass() throws Exception {
    ClientPublicKeyProvider client = mock(ClientPublicKeyProvider.class);
    when(client.getPublicKey(Mockito.eq(EXISTING_CLIENT_APP_UUID))).thenReturn(MAuthKeysHelper.getPublicKeyFromString(PUBLIC_KEY));
    when(mockEpochTimeProvider.inSeconds()).thenReturn(Long.parseLong(CLIENT_NO_BODY_X_MWS_TIME_HEADER_VALUE) + 3);
    Authenticator authenticator = new RequestAuthenticator(client, REQUEST_VALIDATION_TIMEOUT_SECONDS, mockEpochTimeProvider);
    MAuthRequest request = getRequestWithoutMessageBody();

    boolean validationResult = authenticator.authenticate(request);

    assertThat(validationResult, equalTo(true));
  }

}