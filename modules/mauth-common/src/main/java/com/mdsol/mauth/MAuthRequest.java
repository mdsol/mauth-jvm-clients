package com.mdsol.mauth;

import com.mdsol.mauth.util.MAuthHeadersHelper;

import org.apache.commons.lang3.StringUtils;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

/**
 * Wrapper for an incoming MAuth request data necessary to authenticate it.
 */
public class MAuthRequest {

  /* @deprecated
   * This is the header name for Mauth V1 protocol, replaced by MCC_TIME_HEADER_NAME for Mauth V2 protocol
   */
  public static final String X_MWS_TIME_HEADER_NAME = "x-mws-time";

  /* @deprecated
   * This is the header name for Mauth V1 protocol, replaced by MCC_AUTHENTICATION_HEADER_NAME for Mauth V2 protocol
   */
  public static final String X_MWS_AUTHENTICATION_HEADER_NAME = "x-mws-authentication";

  public static final String MCC_TIME_HEADER_NAME = "mcc-time";
  public static final String MCC_AUTHENTICATION_HEADER_NAME = "mcc-authentication";
  private static final String VALIDATION_EXCEPTION_MESSAGE_TEMPLATE = "%s cannot be null or empty.";

  private final UUID appUUID;
  private final String requestSignature;
  private byte[] messagePayload;
  private final String httpMethod;
  private final long requestTime;
  private final String resourcePath;
  private final String queryParameters;
  private final MAuthVersion mauthVersion;
  private String xmwsSignature = null;
  private String xmwsTime = null;
  private InputStream bodyInputStream = null;

  /**
   * Create a Mauth request
   *
   * @deprecated
   *   This is used for Mauth V1 protocol,
   *   replaced by {@link #MAuthRequest(String authenticationHeaderValue, byte[] messagePayload, String httpMethod,
   *       String timeHeaderValue, String resourcePath, String queryParameters)} for Mauth V2 protocol
   *
   * @param authenticationHeaderValue the string value of Mauth authentication Header
   * @param messagePayload byte[] of request payload
   * @param httpMethod the string value of Http_Verb
   * @param timeHeaderValue the string value of Mauth time Header
   * @param resourcePath resource_url_path (no host, port or query string; first "/" is included)
   */
  @Deprecated
  public MAuthRequest(String authenticationHeaderValue, byte[] messagePayload, String httpMethod,
       String timeHeaderValue, String resourcePath) {
    this(authenticationHeaderValue, messagePayload, httpMethod, timeHeaderValue, resourcePath, "");
  }

  /**
   * Create a Mauth request
   *
   * @param authenticationHeaderValue the string value of Mauth authentication Header
   * @param messagePayload byte[] of request payload
   * @param httpMethod the string value of Http_Verb
   * @param timeHeaderValue the string value of Mauth time Header
   * @param resourcePath resource_url_path (no host, port or query string; first "/" is included)
   * @param queryParameters the string value of request parameters
   */
  public MAuthRequest(String authenticationHeaderValue, byte[] messagePayload, String httpMethod,
      String timeHeaderValue, String resourcePath, String queryParameters) {
    this(authenticationHeaderValue, messagePayload, null, httpMethod, timeHeaderValue, resourcePath, queryParameters);
  }

  /**
   * Create a Mauth request
   *
   * @param authenticationHeaderValue the string value of Mauth authentication Header
   * @param bodyInputStream InputStream of request payload
   * @param httpMethod the string value of Http_Verb
   * @param timeHeaderValue the string value of Mauth time Header
   * @param resourcePath resource_url_path (no host, port or query string; first "/" is included)
   * @param queryParameters the string value of request parameters
   */
  public MAuthRequest(String authenticationHeaderValue, InputStream bodyInputStream, String httpMethod,
                      String timeHeaderValue, String resourcePath, String queryParameters) {
    this(authenticationHeaderValue, null, bodyInputStream, httpMethod, timeHeaderValue, resourcePath, queryParameters);
  }

  /**
   * Create a Mauth request
   *
   * @param authenticationHeaderValue the string value of Mauth authentication Header
   * @param messagePayload byte[] of request payload
   * @param bodyInputStream InputStream of request payload
   * @param httpMethod the string value of Http_Verb
   * @param timeHeaderValue the string value of Mauth time Header
   * @param resourcePath resource_url_path (no host, port or query string; first "/" is included)
   * @param queryParameters the string value of request parameters
   */
  private MAuthRequest(String authenticationHeaderValue, byte[] messagePayload, InputStream bodyInputStream, String httpMethod,
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
    if (queryParameters == null) {
      queryParameters = "";
    }

    this.appUUID = appUUID;
    this.requestSignature = requestSignature;
    this.httpMethod = httpMethod;
    this.requestTime = requestTime;
    this.resourcePath = resourcePath;
    this.queryParameters = queryParameters;
    this.mauthVersion = MAuthHeadersHelper.getMauthVersion(authenticationHeaderValue);

    // we always use inputStream for Auth,
    // wrap byte array in ByteArrayInputStream if bodyInputStream wasn't provided explicitly
    if (bodyInputStream != null && messagePayload != null) {
      throw new IllegalArgumentException("Only one of bodyInputStream and messagePayload should be provided.");
    }

    if (messagePayload == null && bodyInputStream == null) {
      // Set payload to empty byte[]
      messagePayload = new byte[] {};
    }

    if (messagePayload != null) {
      // Use our body bytes as inputStream
      this.messagePayload = messagePayload;
      this.bodyInputStream = new ByteArrayInputStream(messagePayload);
    } else {
      this.messagePayload = null;
      this.bodyInputStream = bodyInputStream;
    }
  }

  public UUID getAppUUID() {
    return appUUID;
  }

  public String getRequestSignature() {
    return requestSignature;
  }

  public byte[] getMessagePayload() {
    if (messagePayload != null)
      return Arrays.copyOf(messagePayload, messagePayload.length);
    else
      return null;
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

  public String getXmwsSignature() {
    return xmwsSignature;
  }

  public void setXmwsSignature(String xmwsSignature) {
    this.xmwsSignature = xmwsSignature;
  }

  public String getXmwsTime() {
    return xmwsTime;
  }

  public void setXmwsTime(String xmwsTime) {
    this.xmwsTime = xmwsTime;
  }

  public MAuthVersion getMauthVersion() {
    return mauthVersion;
  }

  public InputStream getBodyInputStream() {
    return bodyInputStream;
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
    private TreeMap<String, String> mauthHeaders = new TreeMap<String,String>(String.CASE_INSENSITIVE_ORDER);
    private InputStream bodyInputStream = null;

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

    public Builder withBodyInputStream(InputStream bodyInputStream) {
      this.bodyInputStream = bodyInputStream;
      return this;
    }

    /**
     * Set Mauth headers (it may include the both sets of Mauth V1 and V2)
     * @param mauthHeaders the request headers for Mauth, such as
     *        Mauth V1, X_MWS_AUTHENTICATION_HEADER_NAME, X_MWS_TIME_HEADER_NAME
     *        Mauth V2, MCC_AUTHENTICATION_HEADER_NAME and MCC_TIME_HEADER_NAME
     *
     * @return Builder
     */
    public Builder withMauthHeaders(Map<String, String> mauthHeaders) {
      this.mauthHeaders.putAll(mauthHeaders);
      return this;
    }

    /**
     * Construct a MAuthRequest object
     *
     * If mauthHeaders are provided, get the value of the highest protocol version to construct object
     *
     * @return a object of MAuthRequest
     */
    public MAuthRequest build() {
      // get the newest mauth version from the request headers...
      if (mauthHeaders != null && !mauthHeaders.isEmpty()) {
        if (mauthHeaders.get(MCC_AUTHENTICATION_HEADER_NAME) != null) {
          authenticationHeaderValue = mauthHeaders.get(MCC_AUTHENTICATION_HEADER_NAME);
          timeHeaderValue = mauthHeaders.get(MCC_TIME_HEADER_NAME);
        } else {
          authenticationHeaderValue = mauthHeaders.get(X_MWS_AUTHENTICATION_HEADER_NAME);
          timeHeaderValue = mauthHeaders.get(X_MWS_TIME_HEADER_NAME);
        }
      }

      MAuthRequest mAuthRequest = new MAuthRequest(authenticationHeaderValue, messagePayload,
          bodyInputStream, httpMethod, timeHeaderValue, resourcePath, queryParameters);

      if (mAuthRequest.getMauthVersion().equals(MAuthVersion.MWSV2)) {
        mAuthRequest.setXmwsSignature(mauthHeaders.get(X_MWS_AUTHENTICATION_HEADER_NAME));
        mAuthRequest.setXmwsTime(mauthHeaders.get(X_MWS_TIME_HEADER_NAME));
      }

      return mAuthRequest;

    }
  }
}
