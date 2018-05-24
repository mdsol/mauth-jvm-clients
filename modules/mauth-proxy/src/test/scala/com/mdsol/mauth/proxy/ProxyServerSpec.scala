package com.mdsol.mauth.proxy

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import com.mdsol.mauth.MAuthRequest
import com.typesafe.config.ConfigFactory
import org.apache.http.client.methods.HttpGet
import org.apache.http.impl.client.HttpClients
import org.apache.http.impl.conn.DefaultProxyRoutePlanner
import org.apache.http.{HttpHost, HttpStatus}
import org.scalamock.scalatest.MockFactory
import org.scalatest._

class ProxyServerSpec extends FlatSpec with Matchers with MockFactory with BeforeAndAfterAll with BeforeAndAfterEach {
  private val BASE_URL = "http://localhost"
  private val MY_RESOURCE = "/my/resource"
  var service = new WireMockServer(wireMockConfig.dynamicPort)
  val proxyServer = new ProxyServer(new ProxyConfig(ConfigFactory.load.resolve))

  override protected def beforeAll() {
    service.start()
  }

  override def beforeEach() {
    proxyServer.serve()
  }

  override def afterEach() {
    proxyServer.stop()
  }

  override protected def afterAll() {
    service.stop()
  }

  it should "correctly modify request to add MAuth headers" in {
    service.stubFor(
      get(urlEqualTo(MY_RESOURCE))
        .withHeader(MAuthRequest.X_MWS_AUTHENTICATION_HEADER_NAME, containing("MWS "))
        .withHeader(MAuthRequest.X_MWS_TIME_HEADER_NAME, matching(".*"))
        .willReturn(ok("success"))
    )
    val response = execute(proxyServer.getPort, new HttpGet(BASE_URL + ":" + service.port + MY_RESOURCE))
    response.getStatusLine.getStatusCode shouldBe HttpStatus.SC_OK
  }

  it should "override existing MAuth headers" in {
    val WRONG_MAUTH_HEADER = "This is a wrong Mauth Header"
    service.stubFor(
      get(urlEqualTo(MY_RESOURCE))
        .withHeader(MAuthRequest.X_MWS_AUTHENTICATION_HEADER_NAME, notMatching(WRONG_MAUTH_HEADER))
        .withHeader(MAuthRequest.X_MWS_TIME_HEADER_NAME, notMatching(WRONG_MAUTH_HEADER))
        .willReturn(ok("success"))
    )
    val request = new HttpGet(BASE_URL + ":" + service.port + MY_RESOURCE)
    request.addHeader(MAuthRequest.X_MWS_AUTHENTICATION_HEADER_NAME, WRONG_MAUTH_HEADER)
    request.addHeader(MAuthRequest.X_MWS_TIME_HEADER_NAME, WRONG_MAUTH_HEADER)
    val response = execute(proxyServer.getPort, request)
    response.getStatusLine.getStatusCode shouldBe HttpStatus.SC_OK
  }

  private def execute(proxyServerPort: Int, request: HttpGet) = {
    val proxy = new HttpHost("localhost", proxyServerPort)
    val routePlanner = new DefaultProxyRoutePlanner(proxy)
    val httpClient = HttpClients.custom.setRoutePlanner(routePlanner).build
    httpClient.execute(request)
  }
}
