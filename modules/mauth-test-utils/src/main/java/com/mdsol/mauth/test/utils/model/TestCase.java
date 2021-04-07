package com.mdsol.mauth.test.utils.model;

public class TestCase {
  String name;
  String stringToSign;
  String signature;
  AuthenticationHeader authHeader;
  UnsignedRequest unsignedRequest;
  CaseType caseType;

  public TestCase(String name) {
    this.name = name;
  }

  public String getName() { return name; }

  public CaseType getCaseType() { return caseType; }

  public void setCaseType(CaseType caseType) { this.caseType = caseType; }

  public String getStringToSign() { return stringToSign; }

  public void setStringToSign(String stringToSign) { this.stringToSign = stringToSign; }

  public String getSignature() { return signature; }

  public void setSignature(String signature) { this.signature = signature; }

  public UnsignedRequest getUnsignedRequest() { return unsignedRequest; }

  public void setUnsignedRequest(UnsignedRequest unsignedRequest) { this.unsignedRequest = unsignedRequest; }

  public AuthenticationHeader getAuthenticationHeader() { return authHeader; }

  public void setAuthenticationHeader(AuthenticationHeader authHeader) { this.authHeader = authHeader; }
}
