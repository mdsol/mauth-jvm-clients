package com.mdsol.mauth.utils;

import com.mdsol.mauth.HttpClientRequestSigner;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.protocol.HttpContext;

import java.io.IOException;

public class SignerHttpRequestInterceptor implements HttpRequestInterceptor {

  private final HttpClientRequestSigner signer;

  public SignerHttpRequestInterceptor(HttpClientRequestSigner signer) {
    this.signer = signer;
  }

  @Override
  public void process(HttpRequest httpRequest, HttpContext httpContext) throws HttpException, IOException {
    signer.signRequest((HttpUriRequest) httpRequest);
  }
}
