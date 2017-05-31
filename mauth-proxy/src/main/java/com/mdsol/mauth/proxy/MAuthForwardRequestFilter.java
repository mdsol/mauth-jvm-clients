package com.mdsol.mauth.proxy;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdsol.mauth.Signer;
import com.mdsol.util.BuildInfoService;
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

public class MAuthForwardRequestFilter extends HttpFiltersAdapter {

  private static final Logger logger = LoggerFactory.getLogger(MAuthForwardRequestFilter.class);
  private final MAuthHttpRequestSigner requestSigner;
  private final String buildInfo;

  public MAuthForwardRequestFilter(HttpRequest originalRequest, Signer httpClientRequestSigner) {
    super(originalRequest);
    this.requestSigner = new MAuthHttpRequestSigner(httpClientRequestSigner);

    String tmpBuildInfo;
    try {
      tmpBuildInfo = new ObjectMapper().writeValueAsString(new BuildInfoService().getBuildInfo());
    } catch (JsonProcessingException e) {
      tmpBuildInfo = "Couldn't read build information";
      logger.error(tmpBuildInfo, e);
    }
    buildInfo = tmpBuildInfo;
  }
/* TODO: Getting direct requests to proxy doesn't work for HTTPS calls, need to figure out the problem
  @Override
  public HttpResponse clientToProxyRequest(HttpObject httpObject) {
    if (httpObject instanceof HttpRequest) {
      HttpRequest request = (HttpRequest) httpObject;
      if (request.getMethod().equals(HttpMethod.GET) ||
          request.getMethod().equals(HttpMethod.HEAD)) {
        try {
          final URI uri = new URI(request.getUri());
          if (null == uri.getScheme() && null == uri.getHost() && -1 == uri.getPort()) {
            return appStatus();
          }
        } catch (URISyntaxException e) {
          logger.error("Couldn't parse request uri", e);
        }
      }
    }
    return null;
  }
*/
  @Override
  public HttpResponse proxyToServerRequest(HttpObject httpObject) {
    if (httpObject instanceof FullHttpRequest) {
      final FullHttpRequest request = (FullHttpRequest) httpObject;
      if (!request.getMethod().name().equalsIgnoreCase("CONNECT")) {
        requestSigner.signRequest(request);
      }
    }
    return null;
  }



  private HttpResponse appStatus() {
    byte[] bodyInBytes = buildInfo.getBytes(Charset.forName("UTF-8"));
    ByteBuf content = Unpooled.copiedBuffer(bodyInBytes);
    HttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, content);
    response.headers().set(HttpHeaders.Names.CONTENT_LENGTH, bodyInBytes.length);
    response.headers().set("Content-Type", "application/json; charset=UTF-8");
    response.headers().set("Date", ProxyUtils.formatDate(new Date()));
    response.headers().set(HttpHeaders.Names.CONNECTION, "close");
    return response;
  }
}
