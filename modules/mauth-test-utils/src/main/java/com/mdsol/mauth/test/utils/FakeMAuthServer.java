package com.mdsol.mauth.test.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.apache.http.HttpHeaders;

import java.io.IOException;
import java.util.UUID;

public class FakeMAuthServer {

  public final static UUID EXISTING_CLIENT_APP_UUID = UUID.fromString("162a4b03-4db9-4631-9d1f-d7195f37128d");
  public final static UUID NON_EXISTING_CLIENT_APP_UUID = UUID.fromString("bba1869e-c80d-4f06-8775-6c4ebb0758e0");

  private static final String FIXTURES_MAUTH_SECURITY_TOKEN_RESPONSE_JSON = "fixtures/mauth_security_token_response.json";
  private static WireMockServer wireMockServer = null;

  public static void start(int port) {
    if (null != wireMockServer && wireMockServer.isRunning()) {
      wireMockServer.stop();
    }
    wireMockServer = new WireMockServer(WireMockConfiguration.wireMockConfig().port(port));
    wireMockServer.start();
    WireMock.configureFor("localhost", port);
  }

  public static void stop() {
    if (wireMockServer.isRunning()) {
      wireMockServer.stop();
    }
  }

  public static void resetMappings() {
    wireMockServer.resetMappings();
  }

  public static void return200() {
    WireMock.stubFor(WireMock.get(WireMock.urlPathEqualTo("/mauth/v1/security_tokens/" + EXISTING_CLIENT_APP_UUID.toString() + ".json"))
        .willReturn(WireMock.aResponse().withStatus(200).withBody(mockedMauthTokenResponse())
          .withHeader(HttpHeaders.CACHE_CONTROL.toLowerCase(), "max-age=3600, private")));
  }

  public static void return401() {
    WireMock.stubFor(WireMock.get(WireMock.urlPathEqualTo("/mauth/v1/security_tokens/" + NON_EXISTING_CLIENT_APP_UUID.toString() + ".json"))
        .willReturn(WireMock.aResponse().withStatus(401).withBody("Invalid headers")));
  }

  private static String mockedMauthTokenResponse() {
    String response = null;
    final ObjectMapper mapper = new ObjectMapper();
    try {
      response = mapper.writeValueAsString(mapper.readTree(FakeMAuthServer.class.getClassLoader()
          .getResourceAsStream(FIXTURES_MAUTH_SECURITY_TOKEN_RESPONSE_JSON)));
    } catch (IOException e) {
      e.printStackTrace();
    }
    return response;
  }
}
