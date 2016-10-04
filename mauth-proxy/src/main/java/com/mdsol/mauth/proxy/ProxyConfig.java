package com.mdsol.mauth.proxy;

import com.typesafe.config.Config;

import java.util.UUID;

public class ProxyConfig {

  private final int proxyPort;
  private final int bufferSizeInByes;
  private final String forwardBaseUrl;
  private final String privateKeyFile;
  private final UUID appUuid;

  public ProxyConfig(Config config) {
    this(
        config.getInt("proxy.port"),
        config.getInt("proxy.buffer_size_in_bytes"),
        config.getString("proxy.forward.base_url"),
        UUID.fromString(config.getString("app.uuid")),
        config.getString("app.private_key_file")
    );
  }

  public ProxyConfig(int proxyPort, int bufferSizeInByes, String forwardBaseUrl, UUID appUuid, String privateKeyFile) {
    this.proxyPort = proxyPort;
    this.bufferSizeInByes = bufferSizeInByes;
    this.forwardBaseUrl = forwardBaseUrl;
    this.appUuid = appUuid;
    this.privateKeyFile = privateKeyFile;
  }


  public int getProxyPort() {
    return proxyPort;
  }

  public int getBufferSizeInByes() {
    return bufferSizeInByes;
  }

  public String getForwardBaseUrl() {
    return forwardBaseUrl;
  }

  public String getPrivateKeyFile() {
    return privateKeyFile;
  }

  public UUID getAppUuid() {
    return appUuid;
  }
}
