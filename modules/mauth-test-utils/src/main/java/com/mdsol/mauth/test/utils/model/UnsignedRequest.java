package com.mdsol.mauth.test.utils.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.nio.charset.StandardCharsets;

public class UnsignedRequest {
  @JsonProperty("verb")
  String httpVerb;
  @JsonProperty("url")
  String urlString;
  @JsonProperty("body_filepath")
  String bodyFilepath;
  @JsonProperty("body")
  String body;

  String resourcePath;
  String queryString;
  byte[] bodyInBytes;

  public String getHttpVerb() { return httpVerb; }

  public void setHttpVerb(String httpVerb) { this.httpVerb = httpVerb; }

  public String getUrlString() { return urlString; }

  public void setUrlString(String urlString) {
    this.urlString = urlString;
    int idx = urlString.indexOf('?');
    if (idx == -1 || idx == urlString.length()-1) {
      this.resourcePath = urlString;
      this.queryString = "";
    }
    else {
      this.resourcePath = urlString.substring(0, idx);
      this.queryString = urlString.substring(idx+1);
    }
  }

  public String getBody() { return body; }

  public void setBody(String body) { this.body = body == null? "" : body; }

  public byte[] getBodyInBytes() { return bodyInBytes; }

  public void setBBodyInBytes(byte[] bodyInBytes) {
    this.bodyInBytes = bodyInBytes == null? "".getBytes(StandardCharsets.UTF_8) : bodyInBytes;
  }

  public String getBodyFilepath() { return bodyFilepath; }

  public void setBodyFilepath(String bodyFilepath) { this.bodyFilepath = bodyFilepath; }

  public String getResourcePath() { return resourcePath; }

  public String getQueryString() { return queryString; }
}
