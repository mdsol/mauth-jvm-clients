package com.mdsol.mauth;

import com.typesafe.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class SignerConfiguration implements MAuthConfiguration {
  private static final Logger logger = LoggerFactory.getLogger(SignerConfiguration.class);

  public static final String APP_SECTION_HEADER = "app";
  public static final String MAUTH_SECTION_HEADER = "mauth";
  public static final String APP_UUID_PATH = APP_SECTION_HEADER + ".uuid";
  public static final String APP_PRIVATE_KEY_PATH = APP_SECTION_HEADER + ".private_key";
  public static final String MAUTH_SIGN_VERSIONS = MAUTH_SECTION_HEADER + ".sign_versions";

  public static final List<MAuthVersion> ALL_SIGN_VERSIONS = Arrays.asList(MAuthVersion.values());
  public static final List<MAuthVersion> DEFAULT_SIGN_VERSION = Arrays.asList(MAuthVersion.MWSV2);

  private final UUID appUUID;
  private final transient String privateKey;
  private List<MAuthVersion> signVersions;

  public SignerConfiguration(Config config) {
    this( UUID.fromString(config.getString(APP_UUID_PATH)),
        config.getString(APP_PRIVATE_KEY_PATH),
        config.hasPath(MAUTH_SIGN_VERSIONS) ? config.getString(MAUTH_SIGN_VERSIONS) : "");
  }

  public SignerConfiguration(UUID appUUID, String privateKey) {
    this(appUUID, privateKey, DEFAULT_SIGN_VERSION);
  }

  public SignerConfiguration(UUID appUUID, String privateKey, String signVersionsStr) {
    validateNotNull(appUUID, "Application UUID");
    validateNotBlank(privateKey, "Application Private key");
    this.appUUID = appUUID;
    this.privateKey = privateKey;
    this.signVersions = getSignVersions(signVersionsStr);
  }

  public SignerConfiguration(UUID appUUID, String privateKey, List<MAuthVersion> signVersions) {
    validateNotNull(appUUID, "Application UUID");
    validateNotBlank(privateKey, "Application Private key");
    this.appUUID = appUUID;
    this.privateKey = privateKey;
    this.signVersions = signVersions;
  }

  public UUID getAppUUID() { return appUUID; }

  public String getPrivateKey() {
    return privateKey;
  }

  public List<MAuthVersion> getSignVersions() {
    return signVersions;
  }

  static public List<MAuthVersion> getSignVersions(String signVersionsStr) {
    List<MAuthVersion> signVersions = new ArrayList();
    List<String> unrecognizedVersions = new ArrayList();
    List<String> versionList = Arrays.asList(signVersionsStr.trim().toLowerCase().split(","));
    versionList.forEach(e -> {
      switch (e.trim()) {
        case "v1":
          signVersions.add(MAuthVersion.MWS);
          break;
        case "v2":
          signVersions.add(MAuthVersion.MWSV2);
          break;
        default:
          unrecognizedVersions.add(e.trim());
          break;
      }
    });

    if (signVersions.isEmpty()) return DEFAULT_SIGN_VERSION;

    if (!unrecognizedVersions.isEmpty())
      logger.warn("unrecognized versions to sign requests: " + unrecognizedVersions.toString());

    logger.info("Protocol versions to sign requests: " + signVersions.toString());

    return signVersions;
  }
}
