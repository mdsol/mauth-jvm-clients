package com.mdsol.mauth.api;

import com.mdsol.mauth.domain.MAuthRequest;
import com.mdsol.mauth.exceptions.MAuthSigningException;
import com.mdsol.mauth.exceptions.MAuthValidationException;

import org.apache.http.client.methods.HttpUriRequest;

import java.util.Map;

public interface MAuthService {

  boolean validate(MAuthRequest mAuthRequest) throws MAuthValidationException;

  public Map<String, String> generateHeaders(String httpVerb, String requestPath,
      String requestBody) throws MAuthSigningException;

  public void signRequest(HttpUriRequest request) throws MAuthSigningException;
}
