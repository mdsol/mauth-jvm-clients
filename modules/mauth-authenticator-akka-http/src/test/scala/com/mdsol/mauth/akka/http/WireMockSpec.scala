package com.mdsol.mauth.akka.http

import com.github.tomakehurst.wiremock.client.WireMock
import com.mdsol.mauth.test.utils.{FakeMAuthServer, PortFinder}
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, TestSuite}

trait WireMockSpec extends BeforeAndAfterAll with BeforeAndAfterEach { self: TestSuite =>

  val wireMockPort: Int = PortFinder.findFreePort()

  val testWireMockServer = new FakeMAuthServer
  implicit val wireMockClient: WireMock = new WireMock(wireMockPort)

  override protected def beforeAll(): Unit = {
//    testWireMockServer.start(900)
  }

  override protected def afterAll(): Unit = {
//    testWireMockServer.stop()
  }

  override protected def beforeEach(): Unit = {
    wireMockClient.resetMappings()
    wireMockClient.resetRequests()
  }
}