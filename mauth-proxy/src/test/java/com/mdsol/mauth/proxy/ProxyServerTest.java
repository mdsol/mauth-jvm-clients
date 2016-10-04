package com.mdsol.mauth.proxy;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.mdsol.mauth.MAuthRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClients;
import org.junit.Rule;
import org.junit.Test;

import java.io.IOException;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

public class ProxyServerTest {

  private static final String BASE_URL = "http://localhost";
  private static final String MY_RESOURCE = "/my/resource";

  @Rule
  public WireMockRule service1 = new WireMockRule(wireMockConfig().dynamicPort());

  @Test
  public void shouldCorrectlyModifyRequestWhenForwardUrlHeaderIsNull() throws IOException {
    service1.stubFor(get(urlEqualTo(MY_RESOURCE))
        .withHeader(MAuthRequest.MAUTH_AUTHENTICATION_HEADER_NAME, containing("MWS "))
        .withHeader(MAuthRequest.MAUTH_TIME_HEADER_NAME, matching(".*"))
        .willReturn(aResponse()
            .withStatus(HttpStatus.SC_OK)
            .withBody("success")));

    ProxyServer proxyServer = getProxyServer();

    final int proxyPort = proxyServer.getPort();

    HttpResponse response = HttpClients.createDefault().execute(new HttpGet(BASE_URL + ":" + proxyPort + MY_RESOURCE));
    assert (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK);
    proxyServer.stop();
  }

  @Test
  public void shouldCorrectlyModifyRequestWhenForwardUrlHeaderIsPassed() throws IOException {
    service1.stubFor(get(urlEqualTo(MY_RESOURCE))
        .willReturn(aResponse()
            .withStatus(500)
            .withBody("failed")));

    WireMockServer service2 = new WireMockServer(wireMockConfig().dynamicPort());
    service2.start();
    final String forwardBaseUrl = BASE_URL + ":" + service2.port();

    service2.stubFor(get(urlEqualTo(MY_RESOURCE))
        .withHeader(MAuthRequest.MAUTH_AUTHENTICATION_HEADER_NAME, containing("MWS "))
        .withHeader(MAuthRequest.MAUTH_TIME_HEADER_NAME, matching(".*"))
        .withHeader(ProxyServer.HEADER_FORWARD_BASE_URL, equalTo(forwardBaseUrl))
        .willReturn(aResponse()
            .withStatus(200)
            .withBody("success")));

    ProxyServer proxyServer = getProxyServer();

    final int proxyPort = proxyServer.getPort();

    final HttpGet request = new HttpGet(BASE_URL + ":" + proxyPort + MY_RESOURCE);
    request.addHeader(ProxyServer.HEADER_FORWARD_BASE_URL, forwardBaseUrl);
    HttpResponse response = HttpClients.createDefault().execute(request);
    assert (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK);

    proxyServer.stop();
    WireMock.reset();
    service2.stop();
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

    final int proxyPort = proxyServer.getPort();

    final HttpGet request = new HttpGet(BASE_URL + ":" + proxyPort + MY_RESOURCE);
    request.addHeader(MAuthRequest.MAUTH_AUTHENTICATION_HEADER_NAME, WRONG_MAUTH_HEADER);
    request.addHeader(MAuthRequest.MAUTH_TIME_HEADER_NAME, WRONG_MAUTH_HEADER);

    HttpResponse response = HttpClients.createDefault().execute(request);
    assert (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK);
    proxyServer.stop();
  }

  private ProxyServer getProxyServer() throws IOException {
    ProxyServer proxyServer = new ProxyServer(new ProxyConfig(
        0,
        (512 * 1024),
        BASE_URL + ":" + service1.port(),
        UUID.randomUUID(),
        "/projects/mauth-java-client/mauth-proxy/src/test/resources/fake_private_key"
    ));
    proxyServer.serve();
    return proxyServer;
  }
}