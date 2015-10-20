package com.mdsol.mauth.domain;

import org.apache.commons.lang3.StringUtils;

import java.util.UUID;

/**
 * Wrapper for data necessary which is mandatory to correctly create and process MAuth requests.
 * Such data should be provided by clients who uses this MAuth service library in order to
 * initialize and configure the MAuth client.
 */
public class MAuthConfiguration {

  private final UUID appUUID;
  private final String publicKey;
  private final String privateKey;
  private final String mAuthUrl;
  private final String mAuthRequestUrlPath;
  private final String securityTokensUrl;

  private MAuthConfiguration(UUID appUUID, String publicKey, String privateKey, String mAuthUrl,
      String mAuthRequestUrlPath, String securityTokensUrl) {
    this.appUUID = appUUID;
    this.publicKey = publicKey;
    this.privateKey = privateKey;
    this.mAuthUrl = mAuthUrl;
    this.mAuthRequestUrlPath = mAuthRequestUrlPath;
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

  public String getMAuthUrl() {
    return mAuthUrl;
  }

  public String getMAuthRequestUrlPath() {
    return mAuthRequestUrlPath;
  }

  public String getSecurityTokensUrl() {
    return securityTokensUrl;
  }

  public static class Builder {
    private UUID appUUID;
    private String publicKey;
    private String privateKey;
    private String mAuthUrl;
    private String mAuthRequestUrlPath;
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

    public Builder withMAuthUrl(String mauthUrl) {
      this.mAuthUrl = mauthUrl;
      return this;
    }

    public Builder withMAuthRequestUrlPath(String mAuthRequestUrlPath) {
      this.mAuthRequestUrlPath = mAuthRequestUrlPath;
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
      if (StringUtils.isBlank(mAuthUrl)) {
        throw new IllegalArgumentException(String.format(exceptionMessageTemplate, "MAuth url"));
      }
      if (StringUtils.isBlank(mAuthRequestUrlPath)) {
        throw new IllegalArgumentException(
            String.format(exceptionMessageTemplate, "MAuth request url path"));
      }
      if (StringUtils.isBlank(securityTokensUrl)) {
        throw new IllegalArgumentException(
            String.format(exceptionMessageTemplate, "Security token url"));
      }

      return new MAuthConfiguration(appUUID, publicKey, privateKey, mAuthUrl, mAuthRequestUrlPath,
          securityTokensUrl);
    }
  }
}
