package com.mdsol.mauth.proxy;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.mdsol.mauth.MAuthRequest;
import com.typesafe.config.ConfigFactory;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.DefaultProxyRoutePlanner;
import org.junit.Rule;
import org.junit.Test;

import java.io.IOException;
import java.net.URISyntaxException;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

public class ProxyServerTest {

  private static final String BASE_URL = "http://localhost";
  private static final String MY_RESOURCE = "/my/resource";

  @Rule
  public WireMockRule service1 = new WireMockRule(wireMockConfig().dynamicPort());

  @Test
  public void shouldCorrectlyModifyRequestToAddMAuthHeaders() throws IOException {
    service1.stubFor(get(urlEqualTo(MY_RESOURCE))
        .withHeader(MAuthRequest.MAUTH_AUTHENTICATION_HEADER_NAME, containing("MWS "))
        .withHeader(MAuthRequest.MAUTH_TIME_HEADER_NAME, matching(".*"))
        .willReturn(aResponse()
            .withStatus(HttpStatus.SC_OK)
            .withBody("success")));

    ProxyServer proxyServer = getProxyServer();

    HttpResponse response = execute(proxyServer.getPort(), new HttpGet(BASE_URL + ":" + service1.port() + MY_RESOURCE));
    assert (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK);
    proxyServer.stop();
  }

  @Test
  public void shouldOverrideExistingMAuthHeaders() throws IOException {
    final String WRONG_MAUTH_HEADER = "This is a wrong Mauth Header";

    service1.stubFor(get(urlEqualTo(MY_RESOURCE))
        .withHeader(MAuthRequest.MAUTH_AUTHENTICATION_HEADER_NAME, notMatching(WRONG_MAUTH_HEADER))
        .withHeader(MAuthRequest.MAUTH_TIME_HEADER_NAME, notMatching(WRONG_MAUTH_HEADER))
        .willReturn(aResponse()
            .withStatus(HttpStatus.SC_OK)
            .withBody("success")));

    ProxyServer proxyServer = getProxyServer();

    final HttpGet request = new HttpGet(BASE_URL + ":" + service1.port() + MY_RESOURCE);
    request.addHeader(MAuthRequest.MAUTH_AUTHENTICATION_HEADER_NAME, WRONG_MAUTH_HEADER);
    request.addHeader(MAuthRequest.MAUTH_TIME_HEADER_NAME, WRONG_MAUTH_HEADER);

    HttpResponse response = execute(proxyServer.getPort(), request);

    assert (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK);
    proxyServer.stop();
  }

  private ProxyServer getProxyServer() throws IOException {
    ProxyServer proxyServer = null;
    try {
      proxyServer = new ProxyServer(new ProxyConfig(ConfigFactory.load().resolve()));
      proxyServer.serve();
    } catch (URISyntaxException e) {
      e.printStackTrace();
    }
    return proxyServer;
  }

  private HttpResponse execute(int proxyServerPort, HttpGet request) throws IOException {
    HttpHost proxy = new HttpHost("localhost", proxyServerPort);
    DefaultProxyRoutePlanner routePlanner = new DefaultProxyRoutePlanner(proxy);
    final CloseableHttpClient httpClient = HttpClients.custom()
        .setRoutePlanner(routePlanner)
        .build();

    return httpClient.execute(request);
  }
}