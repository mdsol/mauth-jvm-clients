package com.mdsol.mauth.domain;

import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.UUID;

/**
 * Wrapper for an incoming MAuth request data necessary to validate it.
 */
public class MAuthRequest {

  private final UUID appUUID;
  private final String requestSignature;
  private final byte[] messagePayload;
  private final String httpMethod;
  private final long requestTime;
  private final String resourcePath;

  private MAuthRequest(UUID appUUID, String requestSignature, byte[] messagePayload,
      String httpMethod, long requestTime, String resourcePath) {
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

  public static final class Builder {

    private UUID appUUID;
    private String requestSignature;
    private byte[] messagePayload;
    private String httpMethod;
    private String requestTime;
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

    public Builder withRequestTime(String requestTime) {
      this.requestTime = requestTime;
      return this;
    }

    public Builder withResourcePath(String resourcePath) {
      this.resourcePath = resourcePath;
      return this;
    }

    public MAuthRequest build() {
      final String exceptionMessageTemplate = "%s cannot be null or empty.";

      if (appUUID == null) {
        throw new IllegalArgumentException(
            String.format(exceptionMessageTemplate, "Application UUID"));
      }
      if (StringUtils.isBlank(requestSignature)) {
        throw new IllegalArgumentException(
            String.format(exceptionMessageTemplate, "Request signature"));
      }
      if (messagePayload == null || messagePayload.length == 0) {
        throw new IllegalArgumentException(
            String.format(exceptionMessageTemplate, "Message payload"));
      }
      if (StringUtils.isBlank(httpMethod)) {
        throw new IllegalArgumentException(String.format(exceptionMessageTemplate, "Http method"));
      }
      if (StringUtils.isBlank(requestTime)) {
        throw new IllegalArgumentException(String.format(exceptionMessageTemplate, "Request time"));
      }
      try {
        if (Long.parseLong(requestTime) < 0) {
          throw new IllegalArgumentException("Request time cannot be negative.");
        }
      } catch (NumberFormatException ex) {
        throw new IllegalArgumentException("Request time must express the epoch time.");
      }
      if (StringUtils.isBlank(resourcePath)) {
        throw new IllegalArgumentException(
            String.format(exceptionMessageTemplate, "Resource path"));
      }
      return new MAuthRequest(appUUID, requestSignature, messagePayload, httpMethod,
          Long.parseLong(requestTime), resourcePath);
    }
  }
}
