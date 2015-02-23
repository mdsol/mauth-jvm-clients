package com.mdsol.mauth;

/**
 * Created by jprice on 23/02/15.
 */
public class CurrentEpochTime implements EpochTime {
  @Override
  public long getSeconds() {
    return System.currentTimeMillis() / 1000;
  }
}
