package com.mdsol.mauth.test.utils.model;

import com.mdsol.mauth.test.utils.ProtocolTestSuiteHelper;

import com.fasterxml.jackson.annotation.JsonProperty;

public class SigningConfig {
  @JsonProperty("app_uuid")
  String appUuid;
  @JsonProperty("request_time")
  String requestTime;
  @JsonProperty("private_key_file")
  String privateKeyFile;

  String privateKey;

  public String getAppUuid() { return appUuid; }

  public void setAppUuid(String appUuid) { this.appUuid = appUuid; }

  public String getRequestTime() { return requestTime; }

  public void setRequestTime(String requestTime) { this.requestTime = requestTime; }

  public String getPrivateKeyFile() { return privateKeyFile; }

  public void setPrivateKeyFile(String privateKeyFile) {
    this.privateKeyFile = privateKeyFile;
    privateKey = ProtocolTestSuiteHelper.getPrivateKey(privateKeyFile);
  }

  public String getPrivateKey() { return privateKey; }
}
