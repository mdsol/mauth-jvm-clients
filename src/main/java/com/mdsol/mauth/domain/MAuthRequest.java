package com.mdsol.mauth.domain;

import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.UUID;

/**
 * Wrapper for an incoming MAuth request data necessary to authenticate it.
 */
public class MAuthRequest {

  private final static String VALIDATION_EXCEPTION_MESSAGE_TEMPLATE = "%s cannot be null or empty.";

  private final UUID appUUID;
  private final String requestSignature;
  private final byte[] messagePayload;
  private final String httpMethod;
  private final long requestTime;
  private final String resourcePath;

  private MAuthRequest(UUID appUUID, String requestSignature, byte[] messagePayload,
      String httpMethod, long requestTime, String resourcePath) {
    validateNotNull(appUUID, "Application UUID");
    validateNotBlank(requestSignature, "Request signature");
    validateNotBlank(httpMethod, "Http method");
    validateNotBlank(resourcePath, "Resource path");
    validateMessagePayload(messagePayload);
    validateRequestTime(requestTime);

    this.appUUID = appUUID;
    this.requestSignature = requestSignature;
    this.messagePayload = messagePayload;
    this.httpMethod = httpMethod;
    this.requestTime = requestTime;
    this.resourcePath = resourcePath;
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

  private void validateNotNull(Object field, String fieldNameInExceptionMessage) {
    if (field == null) {
      throw new IllegalArgumentException(
          String.format(VALIDATION_EXCEPTION_MESSAGE_TEMPLATE, fieldNameInExceptionMessage));
    }
  }

  private void validateNotBlank(String field, String fieldNameInExceptionMessage) {
    if (StringUtils.isBlank(field)) {
      throw new IllegalArgumentException(
          String.format(VALIDATION_EXCEPTION_MESSAGE_TEMPLATE, fieldNameInExceptionMessage));
    }
  }

  private void validateMessagePayload(byte[] messagePayload) {
    if (messagePayload == null || messagePayload.length == 0) {
      throw new IllegalArgumentException(
          String.format(VALIDATION_EXCEPTION_MESSAGE_TEMPLATE, "Message payload"));
    }
  }

  private void validateRequestTime(long requestTime) {
    if (requestTime <= 0) {
      throw new IllegalArgumentException("Request time cannot be negative or 0.");
    }
  }

  public static final class Builder {

    private UUID appUUID;
    private String requestSignature;
    private byte[] messagePayload;
    private String httpMethod;
    private long requestTime;
    private String resourcePath;

    public static Builder get() {
      return new Builder();
    }

    public Builder withAppUUID(UUID appUUID) {
      this.appUUID = appUUID;
      return this;
    }

    public Builder withRequestSignature(String requestSignature) {
      this.requestSignature = requestSignature;
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

    public Builder withRequestTime(long requestTime) {
      this.requestTime = requestTime;
      return this;
    }

    public Builder withResourcePath(String resourcePath) {
      this.resourcePath = resourcePath;
      return this;
    }

    public MAuthRequest build() {
      return new MAuthRequest(appUUID, requestSignature, messagePayload, httpMethod, requestTime,
          resourcePath);
    }
  }
}
