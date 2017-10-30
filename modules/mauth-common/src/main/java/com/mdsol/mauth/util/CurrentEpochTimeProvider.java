package com.mdsol.mauth.util;

/**
 * Basic implementation of {@code EpochTimeProvider} for getting the current time in seconds.
 */
public class CurrentEpochTimeProvider implements EpochTimeProvider {
  @Override
  public long inSeconds() {
    return System.currentTimeMillis() / 1000;
  }
}
