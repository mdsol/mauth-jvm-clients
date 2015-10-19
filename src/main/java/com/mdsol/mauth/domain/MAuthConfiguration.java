package com.mdsol.mauth.domain;

import org.apache.commons.lang.StringUtils;

import java.util.UUID;

public class MAuthConfiguration {

  private final UUID appUUID;
  private final String publicKey;
  private final String privateKey;
  private final String mauthUrl;
  private final String mauthRequestUrlPath;
  private final String securityTokensUrl;

  private MAuthConfiguration(UUID appUUID, String publicKey, String privateKey, String mauthUrl,
      String mauthRequestUrlPath, String securityTokensUrl) {
    this.appUUID = appUUID;
    this.publicKey = publicKey;
    this.privateKey = privateKey;
    this.mauthUrl = mauthUrl;
    this.mauthRequestUrlPath = mauthRequestUrlPath;
    this.securityTokensUrl = securityTokensUrl;
  }

  public UUID getAppUUID() {
    return appUUID;
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

  public static class Builder {
    private UUID appUUID;
    private String publicKey;
    private String privateKey;
    private String mauthUrl;
    private String mauthRequestUrlPath;
    private String securityTokensUrl;

    public static Builder get() {
      return new Builder();
    }

    public Builder withAppUUID(UUID appUUID) {
      this.appUUID = appUUID;
      return this;
    }

    public Builder withPublicKey(String publicKey) {
      this.publicKey = publicKey;
      return this;
    }

    public Builder withPrivateKey(String privateKey) {
      this.privateKey = privateKey;
      return this;
    }

    public Builder withMauthUrl(String mauthUrl) {
      this.mauthUrl = mauthUrl;
      return this;
    }

    public Builder withMauthRequestUrlPath(String mauthRequestUrlPath) {
      this.mauthRequestUrlPath = mauthRequestUrlPath;
      return this;
    }

    public Builder withSecurityTokensUrl(String securityTokensUrl) {
      this.securityTokensUrl = securityTokensUrl;
      return this;
    }

    public MAuthConfiguration build() {
      final String exceptionMessageTemplate = "%s cannot be null or empty.";

      if (appUUID == null) {
        throw new IllegalArgumentException(
            String.format(exceptionMessageTemplate, "Application UUID"));
      }
      if (StringUtils.isBlank(publicKey)) {
        throw new IllegalArgumentException(String.format(exceptionMessageTemplate, "Public key"));
      }
      if (StringUtils.isBlank(privateKey)) {
        throw new IllegalArgumentException(String.format(exceptionMessageTemplate, "Private key"));
      }
      if (StringUtils.isBlank(mauthUrl)) {
        throw new IllegalArgumentException(String.format(exceptionMessageTemplate, "MAuth url"));
      }
      if (StringUtils.isBlank(mauthRequestUrlPath)) {
        throw new IllegalArgumentException(
            String.format(exceptionMessageTemplate, "MAuth request url path"));
      }
      if (StringUtils.isBlank(securityTokensUrl)) {
        throw new IllegalArgumentException(
            String.format(exceptionMessageTemplate, "Security token url"));
      }

      return new MAuthConfiguration(appUUID, publicKey, privateKey, mauthUrl, mauthRequestUrlPath,
          securityTokensUrl);
    }
  }
}
