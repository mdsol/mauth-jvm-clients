package com.mdsol.mauth.proxy;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdso.util.BuildInfoService;
import com.mdsol.mauth.Signer;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.*;
import org.littleshoot.proxy.HttpFiltersAdapter;
import org.littleshoot.proxy.impl.ProxyUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.Date;
import java.util.Map;

public class MAuthRequestFilter extends HttpFiltersAdapter {

  private static final Logger logger = LoggerFactory.getLogger(MAuthRequestFilter.class);
  private final Signer httpClientRequestSigner;
  private final String buildInfo;

  public MAuthRequestFilter(HttpRequest originalRequest, Signer httpClientRequestSigner) {
    super(originalRequest);
    this.httpClientRequestSigner = httpClientRequestSigner;

    ObjectMapper mapper = new ObjectMapper();
    String tmpBuildInfo;
    try {
      tmpBuildInfo = mapper.writeValueAsString(new BuildInfoService().getBuildInfo());
    } catch (JsonProcessingException e) {
      tmpBuildInfo = "Couldn't read build information";
      logger.error(tmpBuildInfo, e);
    }
    buildInfo = tmpBuildInfo;
  }

  @Override
  public HttpResponse clientToProxyRequest(HttpObject httpObject) {
    if(httpObject instanceof HttpRequest) {
      HttpRequest request = (HttpRequest) httpObject;
      if(request.getMethod().equals(HttpMethod.GET)) {
        try {
          final URI uri = new URI(request.getUri());
          if(null == uri.getScheme() && null == uri.getHost() && -1 == uri.getPort()) {
            return appStatus();
          }
        } catch (URISyntaxException e) {
          logger.error("Couldn't parse request uri", e);
        }
      }
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

  private HttpResponse appStatus() {
    byte[] bodyInBytes = buildInfo.getBytes(Charset.forName("UTF-8"));
    ByteBuf content = Unpooled.copiedBuffer(bodyInBytes);
    HttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.BAD_GATEWAY, content);
    response.headers().set(HttpHeaders.Names.CONTENT_LENGTH, bodyInBytes.length);
    response.headers().set("Content-Type", "application/json; charset=UTF-8");
    response.headers().set("Date", ProxyUtils.formatDate(new Date()));
    response.headers().set(HttpHeaders.Names.CONNECTION, "close");
    return response;
  }
}
