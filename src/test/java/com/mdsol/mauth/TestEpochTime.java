package com.mdsol.mauth;

/**
 * @author Jonathan Price <jprice@mdsol.com>
 */
public class TestEpochTime implements EpochTime {
  private final long seconds;

  public TestEpochTime(long seconds) {
    this.seconds = seconds;
  }

  @Override
  public long getSeconds() {
    return seconds;
  }
}
