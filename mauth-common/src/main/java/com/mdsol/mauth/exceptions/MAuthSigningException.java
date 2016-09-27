package com.mdsol.mauth.exceptions;

public class MAuthSigningException extends RuntimeException {

  private static final long serialVersionUID = 2970129260541987284L;

  public MAuthSigningException() {
    super();
  }

  public MAuthSigningException(String message, Throwable cause, boolean enableSuppression,
      boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
  }

  public MAuthSigningException(String message, Throwable cause) {
    super(message, cause);
  }

  public MAuthSigningException(String message) {
    super(message);
  }

  public MAuthSigningException(Throwable cause) {
    super(cause);
  }

}
