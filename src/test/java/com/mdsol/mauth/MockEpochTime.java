package com.mdsol.mauth;

import com.mdsol.mauth.utils.EpochTime;

/**
 * @author Jonathan Price <jprice@mdsol.com>
 */
public class MockEpochTime implements EpochTime {
  private final long seconds;

  public MockEpochTime(long seconds) {
    this.seconds = seconds;
  }

  @Override
  public long getSeconds() {
    return seconds;
  }
}
