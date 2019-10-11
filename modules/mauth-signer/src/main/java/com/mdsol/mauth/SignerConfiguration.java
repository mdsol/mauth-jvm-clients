package com.mdsol.mauth;

import com.typesafe.config.Config;

import java.util.UUID;

public class SignerConfiguration implements MAuthConfiguration {
  public static final String APP_SECTION_HEADER = "app";
  public static final String MAUTH_SECTION_HEADER = "mauth";
  public static final String APP_UUID_PATH = APP_SECTION_HEADER + ".uuid";
  public static final String APP_PRIVATE_KEY_PATH = APP_SECTION_HEADER + ".private_key";
  public static final String DISABLE_MAUTH_V1 = MAUTH_SECTION_HEADER + ".disable_v1";

  private final UUID appUUID;
  private final transient String privateKey;
  private boolean disableV1 = false;

  public SignerConfiguration(Config config) {
    this( UUID.fromString(config.getString(APP_UUID_PATH)),
        config.getString(APP_PRIVATE_KEY_PATH),
        config.hasPath(DISABLE_MAUTH_V1) || config.isEmpty() ? config.getBoolean(DISABLE_MAUTH_V1) : false);
  }

  public SignerConfiguration(UUID appUUID, String privateKey) {
    validateNotNull(appUUID, "Application UUID");
    validateNotBlank(privateKey, "Application Private key");

    this.appUUID = appUUID;
    this.privateKey = privateKey;
  }

  public SignerConfiguration(UUID appUUID, String privateKey, boolean disableV1) {
    validateNotNull(appUUID, "Application UUID");
    validateNotBlank(privateKey, "Application Private key");

    this.appUUID = appUUID;
    this.privateKey = privateKey;
    this.disableV1 = disableV1;
  }

  public UUID getAppUUID() {
    return appUUID;
  }

  public String getPrivateKey() {
    return privateKey;
  }

  public boolean isDisableV1() {
    return disableV1;
  }
}
