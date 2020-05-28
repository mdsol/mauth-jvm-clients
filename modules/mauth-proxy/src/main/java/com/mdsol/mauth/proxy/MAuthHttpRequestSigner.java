package com.mdsol.mauth.proxy;

import com.mdsol.mauth.Signer;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

class MAuthHttpRequestSigner {
  private static final Logger logger = LoggerFactory.getLogger(MAuthHttpRequestSigner.class);
  private final Signer httpClientRequestSigner;

  MAuthHttpRequestSigner(Signer httpClientRequestSigner) {
    this.httpClientRequestSigner = httpClientRequestSigner;
  }

  void signRequest(HttpRequest request) {
    final String verb = request.getMethod().name();
    String requestPayload = null;
    String queryString = null;
    if (request instanceof FullHttpRequest) {
      requestPayload = ((FullHttpRequest)request).content().toString(StandardCharsets.UTF_8);
    }
    String uriString = request.getUri();
    try {
      URI uri = new URI(uriString);
      uriString = uri.getRawPath();
      queryString = uri.getRawQuery();
    } catch (URISyntaxException e) {
      logger.error("Couldn't get request uri", e);
    }

    logger.debug("Generating request headers for Verb: '" + verb + "' URI: '" + uriString +
            "' Payload: " + requestPayload + "' Query parameters: " + queryString);
    Map<String, String> mAuthHeaders = httpClientRequestSigner
            .generateRequestHeaders(verb,
                    uriString,
                    requestPayload != null ? requestPayload.getBytes() : new byte[0],
                    queryString);
    mAuthHeaders.forEach((key, value) -> request.headers().add(key, value));
  }
}
