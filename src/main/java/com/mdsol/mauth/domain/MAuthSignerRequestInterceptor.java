package com.mdsol.mauth.domain;

import com.mdsol.mauth.api.MAuthService;
import com.mdsol.mauth.api.MAuthServiceClient;
import com.typesafe.config.Config;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.protocol.HttpContext;

import java.io.IOException;

public class MAuthSignerRequestInterceptor implements HttpRequestInterceptor {

  private final MAuthService mAuthService;

  public MAuthSignerRequestInterceptor(Config config) {
    this(new MAuthServiceClient(MAuthConfiguration.Builder.parse(config)));
  }

  public MAuthSignerRequestInterceptor(MAuthConfiguration mAuthConfiguration) {
    this(new MAuthServiceClient(mAuthConfiguration));
  }

  public MAuthSignerRequestInterceptor(MAuthService mAuthService) {
    this.mAuthService = mAuthService;
  }

  @Override
  public void process(HttpRequest httpRequest, HttpContext httpContext) throws HttpException, IOException {
    mAuthService.signRequest((HttpUriRequest) httpRequest);
  }
}