package com.mdsol.mauth.proxy;

import com.mdsol.mauth.Signer;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.QueryStringDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
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
      requestPayload = ((FullHttpRequest)request).content().toString(Charset.forName("UTF-8"));
    }
    String uriString = request.getUri();
    QueryStringDecoder queryParams = new QueryStringDecoder(uriString);
    try {
      URI uri = new URI(uriString);
      uriString = uri.getPath();
      queryString = uri.getQuery();
    } catch (URISyntaxException e) {
      logger.error("Couldn't get request uri", e);
    }

    logger.debug("Generating request headers for Verb: '" + verb + "' URI: '" + uriString +
        "' Payload: " + requestPayload + "' Query parameters: " + queryString);
    Map<String, String> mAuthHeaders = httpClientRequestSigner.generateRequestHeaders(verb, uriString, requestPayload, queryString);
    mAuthHeaders.forEach((key, value) -> request.headers().add(key, value));
  }
}
