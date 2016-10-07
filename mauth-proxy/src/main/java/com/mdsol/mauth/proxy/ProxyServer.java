package com.mdsol.mauth.proxy;

import com.mdsol.mauth.apache.HttpClientRequestSigner;
import com.typesafe.config.ConfigFactory;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import org.littleshoot.proxy.HttpFilters;
import org.littleshoot.proxy.HttpFiltersAdapter;
import org.littleshoot.proxy.HttpFiltersSourceAdapter;
import org.littleshoot.proxy.HttpProxyServer;
import org.littleshoot.proxy.extras.SelfSignedMitmManager;
import org.littleshoot.proxy.impl.DefaultHttpProxyServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.Map;

public class ProxyServer {
  private static final Logger logger = LoggerFactory.getLogger(ProxyServer.class);
  private final ProxyConfig proxyConfig;

  private final HttpClientRequestSigner httpClientRequestSigner;
  private HttpProxyServer httpProxyServer;

  public ProxyServer(ProxyConfig proxyConfig) throws IOException, URISyntaxException {
    this.proxyConfig = proxyConfig;

    httpClientRequestSigner = new HttpClientRequestSigner(
        proxyConfig.getAppUuid(),
        proxyConfig.getPrivateKey()
    );

  }

  public void serve() {
    httpProxyServer = DefaultHttpProxyServer.bootstrap()
        .withPort(proxyConfig.getProxyPort())
        .withManInTheMiddle(new SelfSignedMitmManager())
        .withFiltersSource(new HttpFiltersSourceAdapter() {
          @Override
          public int getMaximumRequestBufferSizeInBytes() {
            return proxyConfig.getBufferSizeInByes();
          }

          public HttpFilters filterRequest(HttpRequest originalRequest, ChannelHandlerContext ctx) {
            return new HttpFiltersAdapter(originalRequest) {
              @Override
              public HttpResponse proxyToServerRequest(HttpObject httpObject) {
                if (httpObject instanceof FullHttpRequest) {
                  signRequest((FullHttpRequest) httpObject);
                }
                return null;
              }
            };
          }

        })
        .start();
  }

  public void stop() {
    if (httpProxyServer != null) {
      httpProxyServer.stop();
    }
  }

  public void abort() {
    if (httpProxyServer != null) {
      httpProxyServer.abort();
    }
  }

  public int getPort() {
    return httpProxyServer.getListenAddress().getPort();
  }

  private void signRequest(FullHttpRequest request) {
    final String verb = request.getMethod().name();

    if(!verb.equalsIgnoreCase("CONNECT")) {
      final String requestPayload = request.content().toString(Charset.forName("UTF-8"));
      String uri = request.getUri();
      try {
        uri = new URI(uri).getPath();
      } catch (URISyntaxException e) {
        logger.error("Couldn't get request uri", e);
      }

      logger.debug("Generating request headers for Verb: '" + verb + "' URI: '" + uri + "' Payload: " + requestPayload);
      Map<String, String> mAuthHeaders = httpClientRequestSigner.generateRequestHeaders(verb, uri, requestPayload);
      mAuthHeaders.entrySet().forEach((header) -> request.headers().add(header.getKey(), header.getValue()));
    }
  }

  public static void main(String[] args) {
    try {
      new ProxyServer(new ProxyConfig(ConfigFactory.load().resolve())).serve();
    } catch (IOException | URISyntaxException e) {
      e.printStackTrace();
    }
  }
}
