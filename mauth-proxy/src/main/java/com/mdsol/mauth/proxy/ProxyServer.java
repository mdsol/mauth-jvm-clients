package com.mdsol.mauth.proxy;

import com.mdsol.mauth.apache.HttpClientRequestSigner;
import com.typesafe.config.ConfigFactory;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.*;
import org.littleshoot.proxy.HttpFilters;
import org.littleshoot.proxy.HttpFiltersAdapter;
import org.littleshoot.proxy.HttpFiltersSourceAdapter;
import org.littleshoot.proxy.impl.DefaultHttpProxyServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;

public class ProxyServer {
  private static final Logger logger = LoggerFactory.getLogger(ProxyServer.class);

  private final ProxyConfig proxyConfig;

  private final HttpClientRequestSigner httpClientRequestSigner;

  public ProxyServer(ProxyConfig proxyConfig) throws IOException {
    this.proxyConfig = proxyConfig;

    httpClientRequestSigner = new HttpClientRequestSigner(
        proxyConfig.getAppUuid(),
        new String(Files.readAllBytes(Paths.get(proxyConfig.getPrivateKeyFile())))
    );

  }

  private void serve() {
    DefaultHttpProxyServer.bootstrap()
        .withPort(proxyConfig.getProxyPort())
        .withFiltersSource(new HttpFiltersSourceAdapter() {
          @Override
          public int getMaximumRequestBufferSizeInBytes() {
            return proxyConfig.getBufferSizeInByes();
          }

          public HttpFilters filterRequest(HttpRequest originalRequest, ChannelHandlerContext ctx) {
            return new HttpFiltersAdapter(originalRequest) {
              @Override
              public HttpResponse clientToProxyRequest(HttpObject httpObject) {
                if (httpObject instanceof HttpRequest) {
                  HttpRequest request = (HttpRequest) httpObject;
                  String forwardBaseUrl = proxyConfig.getForwardBaseUrl();
                  if (null != request.headers().get("forward-base-url")) {
                    forwardBaseUrl = request.headers().get("forward-base-url");
                    logger.debug("Using header forward-base-url: " + forwardBaseUrl);
                  }
                  request.setUri(forwardBaseUrl + request.getUri());
                }
                return null;
              }

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

  private void signRequest(FullHttpRequest request) {
    final String requestPayload = request.content().toString(Charset.forName("UTF-8"));
    final String verb = request.getMethod().name();
    final String uri = request.getUri();

    logger.debug("Generating request headers for Verb: '" + verb + "' URI: '" + uri + "' Payload: " + requestPayload);
    Map<String, String> mAuthHeaders = httpClientRequestSigner.generateRequestHeaders(verb, uri, requestPayload);
    mAuthHeaders.entrySet().forEach((header) -> request.headers().add(header.getKey(), header.getValue()));
  }

  public static void main(String[] args) {
    try {
      new ProxyServer(new ProxyConfig(ConfigFactory.load().resolve())).serve();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
