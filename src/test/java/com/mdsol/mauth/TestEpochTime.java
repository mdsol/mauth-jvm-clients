package com.mdsol.mauth;

/**
 * Created by jprice on 23/02/15.
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
