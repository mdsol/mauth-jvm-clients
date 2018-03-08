package com.mdsol.mauth;

import com.typesafe.config.Config;

public class AuthenticatorConfiguration implements MAuthConfiguration{
  public static final String MAUTH_SECTION_HEADER = "mauth";
  public static final String BASE_URL_PATH = MAUTH_SECTION_HEADER + ".base_url";
  public static final String REQUEST_URL_PATH = MAUTH_SECTION_HEADER + ".request_url";
  public static final String TOKEN_URL_PATH = MAUTH_SECTION_HEADER + ".token_url";
  public static final String TIME_TO_LIVE_SECONDS = MAUTH_SECTION_HEADER + ".cache.time_to_live_seconds";

  private final String baseUrl;
  private final String requestUrlPath;
  private final String securityTokensUrlPath;
  private final Long timeToLive;

  public AuthenticatorConfiguration(Config config) {
    this(
        config.getString(BASE_URL_PATH),
        config.getString(REQUEST_URL_PATH),
        config.getString(TOKEN_URL_PATH),
        config.getLong(TIME_TO_LIVE_SECONDS)
    );
  }

  public AuthenticatorConfiguration(String baseUrl, String requestUrlPath, String securityTokensUrlPath, Long timeToLive) {
    validateNotBlank(baseUrl, "MAuth base url");
    validateNotBlank(requestUrlPath, "MAuth request url path");
    validateNotBlank(securityTokensUrlPath, "MAuth Security tokens url path");
    this.baseUrl = baseUrl;
    this.requestUrlPath = requestUrlPath;
    this.securityTokensUrlPath = securityTokensUrlPath;
    this.timeToLive = timeToLive;
  }

  public long getTimeToLive() {
    return timeToLive;
  }

  public String getRequestUrlPath() {
    return requestUrlPath;
  }

  public String getSecurityTokensUrlPath() {
    return securityTokensUrlPath;
  }

  public String getBaseUrl() {
    return baseUrl;
  }
}
