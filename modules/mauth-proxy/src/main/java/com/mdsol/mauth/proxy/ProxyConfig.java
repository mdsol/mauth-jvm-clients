package com.mdsol.mauth.proxy;

import com.typesafe.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

public class ProxyConfig {
  private static final Logger logger = LoggerFactory.getLogger(ProxyConfig.class);

  public static final String DISABLE_MAUTH_V1 = "mauth.disable_v1";
  private final int proxyPort;
  private final int bufferSizeInByes;
  private String privateKey;
  private final UUID appUuid;
  private boolean disableV1 = false;

  public ProxyConfig(Config config) {
    this(
        config.getInt("proxy.port"),
        config.getInt("proxy.buffer_size_in_bytes"),
        UUID.fromString(config.getString("app.uuid")),
        config.getString("app.private_key"),
        config.hasPath(DISABLE_MAUTH_V1) || config.isEmpty() ? config.getBoolean(DISABLE_MAUTH_V1) : false
    );
  }

  public ProxyConfig(int proxyPort, int bufferSizeInByes, UUID appUuid, String privateKey, boolean disableV1) {
    this.proxyPort = proxyPort;
    this.bufferSizeInByes = bufferSizeInByes;
    this.appUuid = appUuid;
    this.privateKey = privateKey;
    this.disableV1 = disableV1;
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
  public boolean isV1Disabled() {
    return disableV1;
  }
}
