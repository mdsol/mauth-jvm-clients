package com.mdsol.mauth.domain;

import com.typesafe.config.Config;
import org.apache.commons.lang3.StringUtils;

import java.util.UUID;

/**
 * Wrapper for data necessary to correctly create and process MAuth requests. Such data should be
 * provided by clients who uses this MAuth service library in order to initialize and configure the
 * MAuth client.
 */
public class MAuthConfiguration {

  private static final String VALIDATION_EXCEPTION_MESSAGE_TEMPLATE = "%s cannot be null or empty.";

  private final UUID appUUID;
  private final String publicKey;
  private final transient String privateKey;
  private final String mAuthUrl;
  private final String mAuthRequestUrlPath;
  private final String securityTokensUrlPath;

  private MAuthConfiguration(UUID appUUID, String publicKey, String privateKey, String mAuthUrl,
      String mAuthRequestUrlPath, String securityTokensUrlPath) {
    validateNotNull(appUUID, "Application UUID");
    validateNotBlank(publicKey, "Public key");
    validateNotBlank(privateKey, "Private key");
    validateNotBlank(mAuthUrl, "MAuth url");
    validateNotBlank(mAuthRequestUrlPath, "MAuth request url path");
    validateNotBlank(securityTokensUrlPath, "Security tokens url path");

    this.appUUID = appUUID;
    this.publicKey = publicKey;
    this.privateKey = privateKey;
    this.mAuthUrl = mAuthUrl;
    this.mAuthRequestUrlPath = mAuthRequestUrlPath;
    this.securityTokensUrlPath = securityTokensUrlPath;
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

  public String getSecurityTokensUrlPath() {
    return securityTokensUrlPath;
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

    private static final String DEFAULT_MAUTH_REQUEST_URL_PATH = "/mauth/v1";
    private static final String DEFAULT_SECURITY_TOKENS_URL_PATH = "/security_tokens/%s.json";

    private UUID appUUID;
    private String publicKey;
    private String privateKey;
    private String mAuthUrl;
    private String mAuthRequestUrlPath;
    private String securityTokensUrlPath;

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

    /**
     * Use {@code withDefaultMAuthPaths()} if you don't know the paths.
     */
    public Builder withMAuthRequestUrlPath(String mAuthRequestUrlPath) {
      this.mAuthRequestUrlPath = mAuthRequestUrlPath;
      return this;
    }

    /**
     * Use {@code withDefaultMAuthPaths()} if you don't know the paths.
     */
    public Builder withSecurityTokensUrlPath(String securityTokensUrlPath) {
      this.securityTokensUrlPath = securityTokensUrlPath;
      return this;
    }

    public Builder withDefaultMAuthPaths() {
      this.mAuthRequestUrlPath = DEFAULT_MAUTH_REQUEST_URL_PATH;
      this.securityTokensUrlPath = DEFAULT_SECURITY_TOKENS_URL_PATH;
      return this;
    }

    public MAuthConfiguration build() {
      return new MAuthConfiguration(appUUID, publicKey, privateKey, mAuthUrl, mAuthRequestUrlPath,
          securityTokensUrlPath);
    }

    public static MAuthConfiguration parse(Config config){
      return new MAuthConfiguration(
          UUID.fromString(config.getString("mauth.app_uuid")),
          config.getString("mauth.public_key"),
          config.getString("mauth.private_key"),
          config.getString("mauth.url"),
          config.getString("mauth.request_url"),
          config.getString("mauth.token_url")
      );
    }
  }
}
