package com.mdsol.mauth.test.utils.model;

public class SigningAuthentication implements TestCase {
  String name;
  String stringToSign;
  String signature;
  AuthenticationHeader authHeader;
  UnsignedRequest unsignedRequest;

  public SigningAuthentication(
      String name,
      UnsignedRequest unsignedRequest,
      AuthenticationHeader authHeader,
      String stringToSign,
      String signature) {
    this.name = name;
    this.unsignedRequest = unsignedRequest;
    this.authHeader = authHeader;
    this.stringToSign = stringToSign;
    this.signature = signature;
  }

  @Override
  public String getName() { return name; }

  @Override
  public CaseType getType() { return CaseType.SIGNING_AUTHENTICATION; }

  public String getStringToSign() { return stringToSign; }

  public void setStringToSign(String stringToSign) { this.stringToSign = stringToSign; }

  public String getSignature() { return signature; }

  public void setSignature(String signature) { this.signature = signature; }

  public UnsignedRequest getUnsignedRequest() { return unsignedRequest; }

  public void setUnsignedRequest(UnsignedRequest unsignedRequest) { this.unsignedRequest = unsignedRequest; }

  public AuthenticationHeader getAuthenticationHeader() { return authHeader; }

  public void setAuthenticationHeader(AuthenticationHeader authHeader) { this.authHeader = authHeader; }
}
