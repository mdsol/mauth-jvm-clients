package com.mdsol.mauth.fake.services;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.mdsol.mauth.MAuthRequest;
import com.mdsol.mauth.Signer;
import org.junit.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.RequestEntity;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

public class AuthenticatorServiceTest {

  private final String DEFAULT_FORWARD_HOST = "http://default-host.com";
  private final String REQUEST_BODY = "Request body";
  private final HttpMethod REQUEST_METHOD = HttpMethod.POST;
  private final URI REQUEST_URL = URI.create("http://host.com/path?query=42");
  private final HttpHeaders REQUEST_HEADERS = new HttpHeaders();
  private final String CONTENT_TYPE = "application/json";

  {
    REQUEST_HEADERS.add("request-header", "Request header value");
  }

  private final Map<String, String> MAUTH_HEADERS = new HashMap<>();

  {
    MAUTH_HEADERS.put(MAuthRequest.MAUTH_AUTHENTICATION_HEADER_NAME, "MWS uuid:signature");
  }

  @Test
  public void shouldCorrectlyModifyRequestWhenForwardUrlHeaderIsNull() {
    // Arrange
    RequestEntity<String> entity = mockRequestEntity();
    Signer signer = mockMAuthSigner();

    // Act
    AuthenticatorService authenticatorService =
        new AuthenticatorService(signer, DEFAULT_FORWARD_HOST);
    RequestEntity<String> modifiedRequest =
        authenticatorService.createModifiedRequest(entity, null, CONTENT_TYPE);

    // Assert
    assertThat(modifiedRequest.getUrl().toString(),
        equalTo(DEFAULT_FORWARD_HOST + "/path?query=42"));
    assertThat(
        modifiedRequest.getHeaders().get(MAuthRequest.MAUTH_AUTHENTICATION_HEADER_NAME).get(0),
        equalTo("MWS uuid:signature"));
  }

  @Test
  public void shouldCorrectlyModifyRequestWhenForwardUrlHeaderIsPassed() {
    // Arrange
    String forwardUrl = "https://forwarded-host.com/forwarded-path?forwarded-query=42";
    RequestEntity<String> entity = mockRequestEntity();
    Signer signer = mockMAuthSigner(URI.create(forwardUrl));

    // Act
    AuthenticatorService authenticatorService =
        new AuthenticatorService(signer, DEFAULT_FORWARD_HOST);
    RequestEntity<String> modifiedRequest =
        authenticatorService.createModifiedRequest(entity, forwardUrl, CONTENT_TYPE);

    // Assert
    assertThat(modifiedRequest.getUrl().toString(), equalTo(forwardUrl));
    assertThat(
        modifiedRequest.getHeaders().get(MAuthRequest.MAUTH_AUTHENTICATION_HEADER_NAME).get(0),
        equalTo("MWS uuid:signature"));
  }

  @Test
  public void shouldOverrideExistingMAuthHeaders() {
    // Arrange
    RequestEntity<String> entity = mockRequestEntity();
    HttpHeaders headersWithMAuthKeys = new HttpHeaders();
    headersWithMAuthKeys.add(MAuthRequest.MAUTH_AUTHENTICATION_HEADER_NAME,
        "MWS old_uuid:old_signature");
    when(entity.getHeaders()).thenReturn(headersWithMAuthKeys);
    Signer signer = mockMAuthSigner();

    // Act
    AuthenticatorService authenticatorService =
        new AuthenticatorService(signer, DEFAULT_FORWARD_HOST);
    RequestEntity<String> modifiedRequest =
        authenticatorService.createModifiedRequest(entity, null, CONTENT_TYPE);

    // Assert
    assertThat(modifiedRequest.getHeaders().get(MAuthRequest.MAUTH_AUTHENTICATION_HEADER_NAME),
        hasSize(1));
    assertThat(
        modifiedRequest.getHeaders().get(MAuthRequest.MAUTH_AUTHENTICATION_HEADER_NAME).get(0),
        equalTo("MWS uuid:signature"));
  }

  private Signer mockMAuthSigner() {
    Signer mAuthService = mock(Signer.class);
    when(mAuthService.generateRequestHeaders(REQUEST_METHOD.name(), REQUEST_URL.getPath(),
        REQUEST_BODY)).thenReturn(MAUTH_HEADERS);
    return mAuthService;
  }

  private Signer mockMAuthSigner(URI forwardUrl) {
    Signer mAuthService = mock(Signer.class);
    when(mAuthService.generateRequestHeaders(REQUEST_METHOD.name(), forwardUrl.getPath(),
        REQUEST_BODY)).thenReturn(MAUTH_HEADERS);
    return mAuthService;
  }

  private RequestEntity<String> mockRequestEntity() {
    @SuppressWarnings("unchecked")
    RequestEntity<String> entity = mock(RequestEntity.class);
    when(entity.getBody()).thenReturn(REQUEST_BODY);
    when(entity.getMethod()).thenReturn(REQUEST_METHOD);
    when(entity.getUrl()).thenReturn(REQUEST_URL);
    when(entity.getHeaders()).thenReturn(REQUEST_HEADERS);
    return entity;
  }

}
