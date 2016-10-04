package com.mdsol.mauth;

import com.typesafe.config.Config;
import org.apache.commons.lang3.StringUtils;

import java.util.UUID;

public class MAuthConfiguration {
  private static final String VALIDATION_EXCEPTION_MESSAGE_TEMPLATE = "%s cannot be null or empty.";
  public static final String MAUTH_SECTION_HEADER = "mauth";
  public static final String APP_SECTION_HEADER = "app";
  public static final String APP_UUID_PATH = APP_SECTION_HEADER + ".uuid";
  public static final String APP_PRIVATE_KEY_PATH = APP_SECTION_HEADER + ".private_key";
  public static final String BASE_URL_PATH = MAUTH_SECTION_HEADER + ".base_url";
  public static final String REQUEST_URL_PATH = MAUTH_SECTION_HEADER + ".request_url";
  public static final String TOKEN_URL_PATH = MAUTH_SECTION_HEADER + ".token_url";
  public static final String TIME_TO_LIVE_SECONDS = MAUTH_SECTION_HEADER + ".cache.time_to_live_seconds";

  private final UUID appUUID;
  private final String baseUrl;
  private final transient String privateKey;
  private final String requestUrlPath;
  private final String securityTokensUrlPath;
  private final Long timeToLive;

  public MAuthConfiguration(Config config) {
    this(
        UUID.fromString(config.getString(APP_UUID_PATH)),
        config.getString(BASE_URL_PATH),
        config.getString(APP_PRIVATE_KEY_PATH),
        config.getString(REQUEST_URL_PATH),
        config.getString(TOKEN_URL_PATH),
        config.getLong(TIME_TO_LIVE_SECONDS)
    );
  }

  public MAuthConfiguration(UUID appUUID, String baseUrl, String privateKey, String requestUrlPath, String securityTokensUrlPath, Long timeToLive) {
    validateNotNull(appUUID, "Application UUID");
    validateNotBlank(privateKey, "Application Private key");
    validateNotBlank(baseUrl, "MAuth base url");
    validateNotBlank(requestUrlPath, "MAuth request url path");
    validateNotBlank(securityTokensUrlPath, "MAuth Security tokens url path");

    this.appUUID = appUUID;
    this.baseUrl = baseUrl;
    this.privateKey = privateKey;
    this.requestUrlPath = requestUrlPath;
    this.securityTokensUrlPath = securityTokensUrlPath;
    this.timeToLive = timeToLive;
  }

  public String getBaseUrl() {
    return baseUrl;
  }

  public String getRequestUrlPath() {
    return requestUrlPath;
  }

  public String getSecurityTokensUrlPath() {
    return securityTokensUrlPath;
  }

  public UUID getAppUUID() {
    return appUUID;
  }

  public String getPrivateKey() {
    return privateKey;
  }

  public Long getTimeToLive() {
    return timeToLive;
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

}
