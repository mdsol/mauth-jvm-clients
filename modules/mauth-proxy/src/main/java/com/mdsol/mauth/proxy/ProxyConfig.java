package com.mdsol.mauth.proxy;

import com.mdsol.mauth.MAuthVersion;

import com.typesafe.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.runtime.Static;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class ProxyConfig {
  private static final Logger logger = LoggerFactory.getLogger(ProxyConfig.class);

  public static final String SIGN_VERSIONS = "mauth.sign_versions";
  public static final String V2_ONLY_AUTHENTICATE = "mauth.v2_only_authenticate";
  private final int proxyPort;
  private final int bufferSizeInByes;
  private String privateKey;
  private final UUID appUuid;
  private List<MAuthVersion> signVersions;
  private boolean v2OnlyAuthenticate = false;

  public ProxyConfig(Config config) {
    this(
      config.getInt("proxy.port"),
      config.getInt("proxy.buffer_size_in_bytes"),
      UUID.fromString(config.getString("app.uuid")),
      config.getString("app.private_key"),
      config.getString(SIGN_VERSIONS),
      config.getBoolean(V2_ONLY_AUTHENTICATE)
    );
  }

  public ProxyConfig(int proxyPort, int bufferSizeInByes, UUID appUuid, String privateKey,
                     String signVersions, boolean v2OnlyAuthenticate) {
    this.proxyPort = proxyPort;
    this.bufferSizeInByes = bufferSizeInByes;
    this.appUuid = appUuid;
    this.privateKey = privateKey;
    this.signVersions = getSignVersions(signVersions);
    this.v2OnlyAuthenticate = v2OnlyAuthenticate;
  }

  public int getProxyPort() {
    return proxyPort;
  }

  public int getBufferSizeInByes() {
    return bufferSizeInByes;
  }

  public String getPrivateKey() {
    return privateKey;
  }

  public UUID getAppUuid() {
    return appUuid;
  }

  public List<MAuthVersion> getSignVersions() { return signVersions; }

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
    if (signVersions.isEmpty())
      signVersions.add(MAuthVersion.MWSV2);

    if (!unrecognizedVersions.isEmpty())
      logger.warn("unrecognized versions to sign requests: " + unrecognizedVersions.toString());

    logger.info("Protocol versions to sign requests: " + signVersions.toString());

    return signVersions;
  }

  public boolean isV2OnlySignAuthenticate() {
    return v2OnlyAuthenticate;
  }

}
