package com.mdsol.mauth.services;

import com.mdsol.mauth.exceptions.MAuthSigningException;

import org.apache.http.client.methods.HttpUriRequest;

import java.util.Map;

public interface MAuthSigner {

  public Map<String, String> generateHeaders(String httpVerb, String requestPath,
      String requestBody) throws MAuthSigningException;

  public void signRequest(HttpUriRequest request) throws MAuthSigningException;


}
