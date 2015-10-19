package com.mdsol.mauth.exceptions;

public class MAuthHttpClientException extends RuntimeException {

  private static final long serialVersionUID = 504056653568715580L;

  public MAuthHttpClientException() {
    super();
  }

  public MAuthHttpClientException(String message, Throwable cause, boolean enableSuppression,
      boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
  }

  public MAuthHttpClientException(String message, Throwable cause) {
    super(message, cause);
  }

  public MAuthHttpClientException(String message) {
    super(message);
  }

  public MAuthHttpClientException(Throwable cause) {
    super(cause);
  }

}
