package com.mdsol.mauth;

import com.typesafe.config.Config;

public class AuthenticatorConfiguration implements MAuthConfiguration{
  public static final String MAUTH_SECTION_HEADER = "mauth";
  public static final String BASE_URL_PATH = MAUTH_SECTION_HEADER + ".base_url";
  public static final String REQUEST_URL_PATH = MAUTH_SECTION_HEADER + ".request_url";
  public static final String TOKEN_URL_PATH = MAUTH_SECTION_HEADER + ".token_url";
  public static final String TIME_TO_LIVE_SECONDS = MAUTH_SECTION_HEADER + ".cache.time_to_live_seconds";
  public static final String V2_ONLY_AUTHENTICATE= MAUTH_SECTION_HEADER + ".v2_only_authenticate";

  private final String baseUrl;
  private final String requestUrlPath;
  private final String securityTokensUrlPath;
  private final Long timeToLive;
  private final boolean v2OnlyAuthenticate;

  public AuthenticatorConfiguration(Config config) {
    this(
        config.getString(BASE_URL_PATH),
        config.getString(REQUEST_URL_PATH),
        config.getString(TOKEN_URL_PATH),
        config.getLong(TIME_TO_LIVE_SECONDS),
        config.getBoolean(V2_ONLY_AUTHENTICATE)
    );
  }

  public AuthenticatorConfiguration(String baseUrl, String requestUrlPath, String securityTokensUrlPath, Long timeToLive) {
    this (baseUrl, requestUrlPath, securityTokensUrlPath, timeToLive, false);
  }

  public AuthenticatorConfiguration(String baseUrl, String requestUrlPath, String securityTokensUrlPath, Long timeToLive, boolean v2OnlyAuthenticate) {
    validateNotBlank(baseUrl, "MAuth base url");
    validateNotBlank(requestUrlPath, "MAuth request url path");
    validateNotBlank(securityTokensUrlPath, "MAuth Security tokens url path");
    this.baseUrl = baseUrl;
    this.requestUrlPath = requestUrlPath;
    this.securityTokensUrlPath = securityTokensUrlPath;
    this.timeToLive = timeToLive;
    this.v2OnlyAuthenticate = v2OnlyAuthenticate;
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

  public boolean isV2OnlyAuthenticate() {
    return v2OnlyAuthenticate;
  }

}
