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
    proxyPort = config.getInt("proxy.port");
    bufferSizeInByes = config.getInt("proxy.buffer_size_in_bytes");
    forwardBaseUrl = config.getString("proxy.forward.base_url");
    privateKeyFile = config.getString("app.private_key_file");
    appUuid = UUID.fromString(config.getString("app.uuid"));
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
