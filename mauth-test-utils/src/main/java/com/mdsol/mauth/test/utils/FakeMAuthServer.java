package com.mdsol.mauth.test.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;

import java.io.IOException;

public class FakeMAuthServer {

  public final static String EXISTING_CLIENT_APP_UUID = "162a4b03-4db9-4631-9d1f-d7195f37128d";
  public final static String NON_EXISTING_CLIENT_APP_UUID = "bba1869e-c80d-4f06-8775-6c4ebb0758e0";

  private static final String FIXTURES_MAUTH_SECURITY_TOKEN_RESPONSE_JSON = "fixtures/mauth_security_token_response.json";
  private static WireMockServer wireMockServer = null;
  private static int PORT_NUMBER;

  public static void start(int port) {
    PORT_NUMBER = port;
    wireMockServer = new WireMockServer(WireMockConfiguration.wireMockConfig().port(port));
    if (wireMockServer.isRunning()) {
      wireMockServer.stop();
    }
    wireMockServer.start();
  }

  public static void stop() {
    if (wireMockServer.isRunning()) {
      wireMockServer.stop();
    }
  }
  
  public static void return200() {
    WireMock.configureFor("localhost", PORT_NUMBER);
    WireMock.reset();
    WireMock.stubFor(WireMock.get(WireMock.urlPathEqualTo("/mauth/v1/security_tokens/" + EXISTING_CLIENT_APP_UUID + ".json"))
        .willReturn(WireMock.aResponse().withStatus(200).withBody(mockedMauthTokenResponse())));
  }

  public static void return401() {
    WireMock.configureFor("localhost", PORT_NUMBER);
    WireMock.reset();
    WireMock.stubFor(WireMock.get(WireMock.urlPathEqualTo("/mauth/v1/security_tokens/" + NON_EXISTING_CLIENT_APP_UUID + ".json"))
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
