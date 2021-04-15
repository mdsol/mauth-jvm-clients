package com.mdsol.mauth.test.utils.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class AuthenticationHeader {
  @JsonProperty("MCC-Authentication")
  String mccAuthentication;
  @JsonProperty("MCC-Time")
  Long mccTime;

  public String getMccAuthentication() { return mccAuthentication; }

  public void setMccAuthentication(String mccAuthentication) { this.mccAuthentication = mccAuthentication; }

  public long getMccTime() { return mccTime; }

  public void setMccTime(Long mccTime) { this.mccTime = mccTime; }
}
