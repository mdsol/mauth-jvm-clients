package com.mdsol.mauth.exceptions;

public class MAuthKeyException extends RuntimeException {

  private static final long serialVersionUID = 6425835744307160873L;

  public MAuthKeyException() {
    super();
  }

  public MAuthKeyException(String message, Throwable cause, boolean enableSuppression,
      boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
  }

  public MAuthKeyException(String message, Throwable cause) {
    super(message, cause);
  }

  public MAuthKeyException(String message) {
    super(message);
  }

  public MAuthKeyException(Throwable cause) {
    super(cause);
  }
}
