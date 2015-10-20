package com.mdsol.mauth.internals.utils;

/**
 * Basic implementation of {@code EpochTime} for getting the current time in seconds.
 */
public class CurrentEpochTime implements EpochTime {
  @Override
  public long getSeconds() {
    return System.currentTimeMillis() / 1000;
  }
}
