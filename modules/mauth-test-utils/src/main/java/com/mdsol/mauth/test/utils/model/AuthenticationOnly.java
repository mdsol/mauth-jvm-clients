package com.mdsol.mauth.test.utils.model;

public class AuthenticationOnly implements TestCase {
  String name;
  AuthenticationHeader authHeader;
  UnsignedRequest unsignedRequest;

  public AuthenticationOnly(
      String name,
      UnsignedRequest unsignedRequest,
      AuthenticationHeader authHeader) {
    this.name = name;
    this.unsignedRequest = unsignedRequest;
    this.authHeader = authHeader;
  }

  @Override
  public String getName() { return name; }

  @Override
  public CaseType getType() { return CaseType.AUTHENTICATION_ONLY; }

  public UnsignedRequest getUnsignedRequest() { return unsignedRequest; }

  public void setUnsignedRequest(UnsignedRequest unsignedRequest) { this.unsignedRequest = unsignedRequest; }

  public AuthenticationHeader getAuthenticationHeader() { return authHeader; }

  public void setAuthenticationHeader(AuthenticationHeader authHeader) { this.authHeader = authHeader; }
}
