package com.mdsol.mauth.proxy;

import com.typesafe.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

public class ProxyConfig {
  private static final Logger logger = LoggerFactory.getLogger(ProxyConfig.class);
  private final int proxyPort;
  private final int bufferSizeInByes;
  private String privateKey;
  private final UUID appUuid;

  public ProxyConfig(Config config) {
    this(
        config.getInt("proxy.port"),
        config.getInt("proxy.buffer_size_in_bytes"),
        UUID.fromString(config.getString("app.uuid")),
        config.getString("app.private_key_file")
    );
  }

  public ProxyConfig(int proxyPort, int bufferSizeInByes, UUID appUuid, String privateKey) {
    this.proxyPort = proxyPort;
    this.bufferSizeInByes = bufferSizeInByes;
    this.appUuid = appUuid;
    this.privateKey = privateKey;
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
}
