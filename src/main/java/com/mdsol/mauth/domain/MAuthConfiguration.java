package com.mdsol.mauth.domain;

public class MAuthConfiguration {

  private final String appId;
  private final String publicKey;
  private final String privateKey;
  private final String mauthUrl;
  private final String mauthRequestUrlPath;
  private final String securityTokensUrl;

  public MAuthConfiguration(String appId, String publicKey, String privateKey, String mauthUrl,
      String mauthRequestUrlPath, String securityTokensUrl) {
    this.appId = appId;
    this.publicKey = publicKey;
    this.privateKey = privateKey;
    this.mauthUrl = mauthUrl;
    this.mauthRequestUrlPath = mauthRequestUrlPath;
    this.securityTokensUrl = securityTokensUrl;
  }
  
  public String getAppId() {
    return appId;
  }

  public String getPublicKey() {
    return publicKey;
  }

  public String getPrivateKey() {
    return privateKey;
  }

  public String getMauthUrl() {
    return mauthUrl;
  }

  public String getMauthRequestUrlPath() {
    return mauthRequestUrlPath;
  }

  public String getSecurityTokensUrl() {
    return securityTokensUrl;
  }

}
