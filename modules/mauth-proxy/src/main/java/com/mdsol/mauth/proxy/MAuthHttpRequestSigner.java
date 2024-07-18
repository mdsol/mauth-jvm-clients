package com.mdsol.mauth.proxy;

import com.mdsol.mauth.Signer;

import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

class MAuthHttpRequestSigner {
  private static final Logger logger = LoggerFactory.getLogger(MAuthHttpRequestSigner.class);
  private final Signer httpClientRequestSigner;

  MAuthHttpRequestSigner(Signer httpClientRequestSigner) {
    this.httpClientRequestSigner = httpClientRequestSigner;
  }

  void signRequest(HttpRequest request) {
    final String verb = request.getMethod().name();

    byte[] requestPayload = null;
    String queryString = null;
    if (request instanceof FullHttpRequest) {
      requestPayload = ((FullHttpRequest) request).content().array();
    }
    String uriString = request.getUri();
    try {
      URI uri = new URI(uriString);
      uriString = uri.getPath();
      queryString = uri.getQuery();
    } catch (URISyntaxException e) {
      logger.error("Couldn't get request uri", e);
    }

    logger.debug("Generating request headers for Verb: '" + verb + "' URI: '" + uriString +
        "' Payload: " + requestPayload.toString() + "' Query parameters: " + queryString);
    Map<String, String> mAuthHeaders = httpClientRequestSigner.generateRequestHeaders(verb, uriString, requestPayload, queryString);
    mAuthHeaders.forEach((key, value) -> request.headers().add(key, value));
  }
}