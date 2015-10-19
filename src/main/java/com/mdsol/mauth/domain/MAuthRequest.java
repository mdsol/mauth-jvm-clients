package com.mdsol.mauth.domain;

public class MAuthRequest {

  private final String appId;
  private final String requestSignature;
  private final byte[] messagePayload;
  private final String httpMethod;
  private final String requestTime;
  private final String resourcePath;

  public MAuthRequest(String appId, String requestSignature, byte[] messagePayload,
      String httpMethod, String requestTime, String resourcePath) {
    this.appId = appId;
    this.requestSignature = requestSignature;
    this.messagePayload = messagePayload;
    this.httpMethod = httpMethod;
    this.requestTime = requestTime;
    this.resourcePath = resourcePath;
  }

  public String getAppId() {
    return appId;
  }

  public String getRequestSignature() {
    return requestSignature;
  }

  public byte[] getMessagePayload() {
    return messagePayload;
  }

  public String getHttpMethod() {
    return httpMethod;
  }

  public String getRequestTime() {
    return requestTime;
  }

  public String getResourcePath() {
    return resourcePath;
  }

}
