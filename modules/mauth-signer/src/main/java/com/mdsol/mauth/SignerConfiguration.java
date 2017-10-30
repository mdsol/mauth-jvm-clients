package com.mdsol.mauth;

import com.typesafe.config.Config;

import java.util.UUID;

public class SignerConfiguration implements MAuthConfiguration {
  public static final String APP_SECTION_HEADER = "app";
  public static final String APP_UUID_PATH = APP_SECTION_HEADER + ".uuid";
  public static final String APP_PRIVATE_KEY_PATH = APP_SECTION_HEADER + ".private_key";

  private final UUID appUUID;
  private final transient String privateKey;

  public SignerConfiguration(Config config) {
    this(
        UUID.fromString(config.getString(APP_UUID_PATH)),
        config.getString(APP_PRIVATE_KEY_PATH)
    );
  }

  public SignerConfiguration(UUID appUUID, String privateKey) {
    validateNotNull(appUUID, "Application UUID");
    validateNotBlank(privateKey, "Application Private key");

    this.appUUID = appUUID;
    this.privateKey = privateKey;
  }

  public UUID getAppUUID() {
    return appUUID;
  }

  public String getPrivateKey() {
    return privateKey;
  }
}
