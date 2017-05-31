package com.mdsol.mauth.proxy;

import com.mdsol.mauth.Signer;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.Map;

public class MAuthHttpRequestSigner {
  private static final Logger logger = LoggerFactory.getLogger(MAuthHttpRequestSigner.class);
  private final Signer httpClientRequestSigner;

  public MAuthHttpRequestSigner(Signer httpClientRequestSigner) {
    this.httpClientRequestSigner = httpClientRequestSigner;
  }

  public void signRequest(HttpRequest request) {
    final String verb = request.getMethod().name();

    String requestPayload = null;
    if (request instanceof FullHttpRequest) {
      requestPayload = ((FullHttpRequest)request).content().toString(Charset.forName("UTF-8"));
    }
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
