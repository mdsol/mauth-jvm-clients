package com.mdsol.mauth.exceptions;

public class MAuthValidationException extends RuntimeException {

  private static final long serialVersionUID = -4115883646959368793L;

  public MAuthValidationException() {
    super();
  }

  public MAuthValidationException(String message, Throwable cause, boolean enableSuppression,
      boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
  }

  public MAuthValidationException(String message, Throwable cause) {
    super(message, cause);
  }

  public MAuthValidationException(String message) {
    super(message);
  }

  public MAuthValidationException(Throwable cause) {
    super(cause);
  }

}
