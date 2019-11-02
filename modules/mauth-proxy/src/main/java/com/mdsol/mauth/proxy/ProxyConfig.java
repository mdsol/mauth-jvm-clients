package com.mdsol.mauth.proxy;

import com.typesafe.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

public class ProxyConfig {
  private static final Logger logger = LoggerFactory.getLogger(ProxyConfig.class);

  public static final String V2_ONLY_SIGN_REQUESTS = "mauth.v2_only_sign_requests";
  public static final String V2_ONLY_AUTHTICATE = "mauth.v2_only_authenticate";
  private final int proxyPort;
  private final int bufferSizeInByes;
  private String privateKey;
  private final UUID appUuid;
  private boolean v2OnlySignRequests = false;
  private boolean v2OnlyAuthenticate = false;

  public ProxyConfig(Config config) {
    this(
      config.getInt("proxy.port"),
      config.getInt("proxy.buffer_size_in_bytes"),
      UUID.fromString(config.getString("app.uuid")),
      config.getString("app.private_key"),
      config.getBoolean(V2_ONLY_SIGN_REQUESTS),
      config.getBoolean(V2_ONLY_AUTHTICATE)
    );
  }

  public ProxyConfig(int proxyPort, int bufferSizeInByes, UUID appUuid, String privateKey,
      boolean v2OnlySignRequests, boolean v2OnlyAuthenticate) {
    this.proxyPort = proxyPort;
    this.bufferSizeInByes = bufferSizeInByes;
    this.appUuid = appUuid;
    this.privateKey = privateKey;
    this.v2OnlySignRequests = v2OnlySignRequests;
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
  public boolean isV2OnlySignRequests() {
    return v2OnlySignRequests;
  }

  public boolean isV2OnlySignAuthenticate() {
    return v2OnlyAuthenticate;
  }

}
