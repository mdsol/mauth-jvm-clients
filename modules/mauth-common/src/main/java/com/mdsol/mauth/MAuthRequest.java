package com.mdsol.mauth;

import com.mdsol.mauth.util.MAuthHeadersHelper;

import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.Map;
import java.util.UUID;

/**
 * Wrapper for an incoming MAuth request data necessary to authenticate it.
 */
public class MAuthRequest {

  public static final String X_MWS_TIME_HEADER_NAME = "x-mws-time";
  public static final String X_MWS_AUTHENTICATION_HEADER_NAME = "x-mws-authentication";
  public static final String MCC_TIME_HEADER_NAME = "mcc-time";
  public static final String MCC_AUTHENTICATION_HEADER_NAME = "mcc-authentication";

  private static final String VALIDATION_EXCEPTION_MESSAGE_TEMPLATE = "%s cannot be null or empty.";

  private final UUID appUUID;
  private final String requestSignature;
  private final byte[] messagePayload;
  private final String httpMethod;
  private final long requestTime;
  private final String resourcePath;
  private final String queryParameters;
  private final MAuthVersion mauthVersion;

  public MAuthRequest(String authenticationHeaderValue, byte[] messagePayload, String httpMethod,
       String timeHeaderValue, String resourcePath) {
    this(authenticationHeaderValue, messagePayload, httpMethod, timeHeaderValue, resourcePath, "");
  }

  public MAuthRequest(String authenticationHeaderValue, byte[] messagePayload, String httpMethod,
      String timeHeaderValue, String resourcePath, String queryParameters) {
    validateNotBlank(authenticationHeaderValue, "Authentication header value");
    validateNotBlank(timeHeaderValue, "Time header value");

    UUID appUUID = MAuthHeadersHelper.getAppUUIDFromAuthenticationHeader(authenticationHeaderValue);
    String requestSignature =
        MAuthHeadersHelper.getSignatureFromAuthenticationHeader(authenticationHeaderValue);

    long requestTime = MAuthHeadersHelper.getRequestTimeFromTimeHeader(timeHeaderValue);

    validateNotBlank(httpMethod, "Http method");
    validateNotBlank(resourcePath, "Resource path");
    validateRequestTime(requestTime);
    if (messagePayload == null) {
      messagePayload = new byte[] {};
    }

    this.appUUID = appUUID;
    this.requestSignature = requestSignature;
    this.messagePayload = messagePayload;
    this.httpMethod = httpMethod;
    this.requestTime = requestTime;
    this.resourcePath = resourcePath;
    this.queryParameters = queryParameters;
    this.mauthVersion = MAuthHeadersHelper.getMauthVersion(authenticationHeaderValue);
  }

  public UUID getAppUUID() {
    return appUUID;
  }

  public String getRequestSignature() {
    return requestSignature;
  }

  public byte[] getMessagePayload() {
    return Arrays.copyOf(messagePayload, messagePayload.length);
  }

  public String getHttpMethod() {
    return httpMethod;
  }

  public long getRequestTime() {
    return requestTime;
  }

  public String getResourcePath() {
    return resourcePath;
  }

  public String getQueryParameters() {
    return queryParameters;
  }

  public MAuthVersion getMauthVersion() {
    return mauthVersion;
  }

  private void validateNotBlank(String field, String fieldNameInExceptionMessage) {
    if (StringUtils.isBlank(field)) {
      throw new IllegalArgumentException(
          String.format(VALIDATION_EXCEPTION_MESSAGE_TEMPLATE, fieldNameInExceptionMessage));
    }
  }

  private void validateRequestTime(long requestTime) {
    if (requestTime <= 0) {
      throw new IllegalArgumentException("Request time cannot be negative or 0.");
    }
  }

  public static final class Builder {

    private String authenticationHeaderValue;
    private byte[] messagePayload;
    private String httpMethod;
    private String timeHeaderValue;
    private String resourcePath;
    private String queryParameters;
    private Map<String, String> headers;

    public static Builder get() {
      return new Builder();
    }

    public Builder withAuthenticationHeaderValue(String authenticationHeaderValue) {
      this.authenticationHeaderValue = authenticationHeaderValue;
      return this;
    }

    public Builder withTimeHeaderValue(String timeHeaderValue) {
      this.timeHeaderValue = timeHeaderValue;
      return this;
    }

    public Builder withMessagePayload(byte[] messagePayload) {
      this.messagePayload = messagePayload;
      return this;
    }

    public Builder withHttpMethod(String httpMethod) {
      this.httpMethod = httpMethod;
      return this;
    }

    public Builder withResourcePath(String resourcePath) {
      this.resourcePath = resourcePath;
      return this;
    }

    public Builder withQueryParameters(String queryParameters) {
      this.queryParameters = queryParameters;
      return this;
    }

    public Builder withRequestHeaders(Map<String, String> headers) {
      this.headers = headers;
      return this;
    }

    public MAuthRequest build() {
      // get the newest mauth version from the request headers...
      if (headers != null && !headers.isEmpty()) {
        if (headers.get(MCC_AUTHENTICATION_HEADER_NAME) != null) {
          authenticationHeaderValue = headers.get(MCC_AUTHENTICATION_HEADER_NAME);
          timeHeaderValue = headers.get(MCC_TIME_HEADER_NAME);
        } else {
          authenticationHeaderValue = headers.get(X_MWS_AUTHENTICATION_HEADER_NAME);
          timeHeaderValue = headers.get(X_MWS_TIME_HEADER_NAME);
        }
      }
      return new MAuthRequest(authenticationHeaderValue, messagePayload, httpMethod,
          timeHeaderValue, resourcePath, queryParameters);
    }
  }
}
