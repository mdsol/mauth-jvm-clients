package com.mdsol.mauth.services;

import com.mdsol.mauth.api.MAuthService;
import com.mdsol.mauth.domain.MAuthConfiguration;
import com.mdsol.mauth.domain.MAuthRequest;

import org.apache.http.client.methods.HttpUriRequest;

import java.io.IOException;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;

public class MAuthServiceClient implements MAuthService {

  private final MAuthConfiguration configuration;
  private final MAuthSigner signer;
  private final MAuthClient client;

  public MAuthServiceClient(MAuthConfiguration configuration)
      throws SecurityException, IOException {
    if (configuration == null) {
      throw new IllegalArgumentException("MAuth configuration cannot be null.");
    }
    this.configuration = configuration;
    this.signer = new MAuthRequestSigner(UUID.fromString(configuration.getAppId()),
        configuration.getPrivateKey());
    this.client = new MAuthHttpClient(configuration, signer);
  }

  @Override
  public boolean validate(MAuthRequest mauthRequest) {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public void signRequest(HttpServletRequest request) {
    // TODO Auto-generated method stub

  }

  @Override
  public void signRequest(HttpUriRequest httpUriRequest) {
    // TODO Auto-generated method stub
  }

  // @Override
  // public void signRequest(RequestEntity<?> requestEntity) {
  // // TODO Auto-generated method stub
  // }

}
