package com.mdsol.mauth.domain;

import org.apache.commons.lang3.StringUtils;

import java.util.UUID;

/**
 * Wrapper for data necessary to correctly create and process MAuth requests. Such data should be
 * provided by clients who uses this MAuth service library in order to initialize and configure the
 * MAuth client.
 */
public class MAuthConfiguration {

  private final static String VALIDATION_EXCEPTION_MESSAGE_TEMPLATE = "%s cannot be null or empty.";

  private final UUID appUUID;
  private final String publicKey;
  private final transient String privateKey;
  private final String mAuthUrl;
  private final String mAuthRequestUrlPath;
  private final String securityTokensUrl;

  private MAuthConfiguration(UUID appUUID, String publicKey, String privateKey, String mAuthUrl,
      String mAuthRequestUrlPath, String securityTokensUrl) {
    validateNotNull(appUUID, "Application UUID");
    validateNotBlank(publicKey, "Public key");
    validateNotBlank(privateKey, "Private key");
    validateNotBlank(mAuthUrl, "MAuth url");
    validateNotBlank(mAuthRequestUrlPath, "MAuth request url path");
    validateNotBlank(securityTokensUrl, "Security token url");

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
      return new MAuthConfiguration(appUUID, publicKey, privateKey, mAuthUrl, mAuthRequestUrlPath,
          securityTokensUrl);
    }
  }
}
