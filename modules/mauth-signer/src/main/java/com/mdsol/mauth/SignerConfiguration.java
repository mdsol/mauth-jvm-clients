package com.mdsol.mauth;

import com.typesafe.config.Config;

import java.util.UUID;

public class SignerConfiguration implements MAuthConfiguration {
  public static final String APP_SECTION_HEADER = "app";
  public static final String MAUTH_SECTION_HEADER = "mauth";
  public static final String APP_UUID_PATH = APP_SECTION_HEADER + ".uuid";
  public static final String APP_PRIVATE_KEY_PATH = APP_SECTION_HEADER + ".private_key";
  public static final String V2_ONLY_SIGN_REQUESTS = MAUTH_SECTION_HEADER + ".v2_only_sign_requests";


  private final UUID appUUID;
  private final transient String privateKey;
  private boolean v2OnlySignRequests = false;

  public SignerConfiguration(Config config) {
    this( UUID.fromString(config.getString(APP_UUID_PATH)),
        config.getString(APP_PRIVATE_KEY_PATH),
        config.hasPath(V2_ONLY_SIGN_REQUESTS) || config.isEmpty() ? config.getBoolean(V2_ONLY_SIGN_REQUESTS) : false);
  }

  public SignerConfiguration(UUID appUUID, String privateKey) {
    validateNotNull(appUUID, "Application UUID");
    validateNotBlank(privateKey, "Application Private key");

    this.appUUID = appUUID;
    this.privateKey = privateKey;
  }

  public SignerConfiguration(UUID appUUID, String privateKey, boolean v2OnlySignRequests) {
    validateNotNull(appUUID, "Application UUID");
    validateNotBlank(privateKey, "Application Private key");

    this.appUUID = appUUID;
    this.privateKey = privateKey;
    this.v2OnlySignRequests = v2OnlySignRequests;
  }

  public UUID getAppUUID() {
    return appUUID;
  }

  public String getPrivateKey() {
    return privateKey;
  }

  public boolean isV2OnlySignRequests() {
    return v2OnlySignRequests;
  }
}
