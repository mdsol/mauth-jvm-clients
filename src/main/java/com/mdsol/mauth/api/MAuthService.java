package com.mdsol.mauth.api;

import com.mdsol.mauth.domain.MAuthRequest;

import org.apache.http.client.methods.HttpUriRequest;

import javax.servlet.http.HttpServletRequest;

public interface MAuthService {

  boolean validate(MAuthRequest mauthRequest);

  /**
   * Appends necessary MAuth headers to the request
   */
  void signRequest(HttpServletRequest request);

  /**
   * Overloaded version of signRequest which works with Apache HttpClient
   */
  void signRequest(HttpUriRequest httpUriRequest);
}
