package com.mdsol.mauth;

/**
 * @author Jonathan Price <jprice@mdsol.com>
 */
public class CurrentEpochTime implements EpochTime {
  @Override
  public long getSeconds() {
    return System.currentTimeMillis() / 1000;
  }
}
