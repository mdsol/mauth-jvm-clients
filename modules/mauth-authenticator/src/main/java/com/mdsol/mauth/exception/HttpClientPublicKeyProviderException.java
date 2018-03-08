package com.mdsol.mauth.exception;

public class HttpClientPublicKeyProviderException extends RuntimeException {

  private static final long serialVersionUID = 504056653568715580L;

  public HttpClientPublicKeyProviderException() {
    super();
  }

  public HttpClientPublicKeyProviderException(String message, Throwable cause, boolean enableSuppression,
                                              boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
  }

  public HttpClientPublicKeyProviderException(String message, Throwable cause) {
    super(message, cause);
  }

  public HttpClientPublicKeyProviderException(String message) {
    super(message);
  }

  public HttpClientPublicKeyProviderException(Throwable cause) {
    super(cause);
  }

}
