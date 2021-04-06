package com.mdsol.mauth.test.utils.model;

public class TestCase {
  String name;
  String stringToSign;
  String signature;
  AuthenticationHeader authHeader;
  UnsignedRequest unsignedRequest;
  boolean authenticationOnly = false;

  public TestCase(String name) {
    this.name = name;
  }

  public String getName() { return name; }

  public boolean isAuthenticationOnly() { return authenticationOnly; }

  public void setAuthenticationOnly(boolean authenticationOnly) { this.authenticationOnly = authenticationOnly; }

  public String getStringToSign() { return stringToSign; }

  public void setStringToSign(String stringToSign) { this.stringToSign = stringToSign; }

  public String getSignature() { return signature; }

  public void setSignature(String signature) { this.signature = signature; }

  public UnsignedRequest getUnsignedRequest() { return unsignedRequest; }

  public void setUnsignedRequest(UnsignedRequest unsignedRequest) { this.unsignedRequest = unsignedRequest; }

  public AuthenticationHeader getAuthenticationHeader() { return authHeader; }

  public void setAuthenticationHeader(AuthenticationHeader authHeader) { this.authHeader = authHeader; }
}
