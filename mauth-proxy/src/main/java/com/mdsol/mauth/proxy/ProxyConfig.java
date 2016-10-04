package com.mdsol.mauth.proxy;

import com.typesafe.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.UUID;

public class ProxyConfig {
  private static final Logger logger = LoggerFactory.getLogger(ProxyConfig.class);
  private final int proxyPort;
  private final int bufferSizeInByes;
  private final String forwardBaseUrl;
  private URI privateKeyFile;
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
    try {
      URI uri = new URI(privateKeyFile);
      if(null == uri.getScheme()){
        uri = new URI("file", uri.getUserInfo(), uri.getHost(), uri.getPort(), uri.getPath(), uri.getQuery(), uri.getFragment());
      } else if(uri.getScheme().equalsIgnoreCase("classpath")){
        uri = ClassLoader.getSystemResource(uri.getPath().replaceAll("^/", "")).toURI();
      }
      this.privateKeyFile = uri;
    } catch (URISyntaxException e) {
      logger.error("Couldn't generate URI from :" + privateKeyFile, e);
    }

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

  public URI getPrivateKeyFile() {
    return privateKeyFile;
  }

  public UUID getAppUuid() {
    return appUuid;
  }
}
