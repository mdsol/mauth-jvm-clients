package com.mdsol.mauth.http

import akka.actor.ActorSystem
import akka.http.scaladsl.model.{HttpRequest, StatusCodes, Uri}
import akka.stream.ActorMaterializer
import brave.{Span, Tracer, Tracing}
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.options
import com.github.tomakehurst.wiremock.matching.RegexPattern
import com.mdsol.mauth.{SignedRequest, UnsignedRequest}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, FlatSpec}
import zipkin2.Endpoint

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.language.postfixOps

class TraceHttpClientSpec extends FlatSpec with TraceHttpClient with BeforeAndAfterAll with BeforeAndAfterEach with ScalaFutures {

  behavior of "TraceHttpClient"

  override implicit val patienceConfig: PatienceConfig = PatienceConfig(timeout = 10 seconds, interval = 100 milliseconds)

  private val wiremock = new WireMockServer(options().dynamicPort())
  private lazy val testUrl = s"http://localhost:${wiremock.port}/test"

  private implicit val actorSystem: ActorSystem = ActorSystem()
  private implicit val materializer: ActorMaterializer = ActorMaterializer()
  private implicit val dispatcher: ExecutionContext = actorSystem.dispatcher

  override implicit val tracer: Tracer = Tracing.newBuilder()
    .endpoint(Endpoint.newBuilder().serviceName("my-service").build())
    .build().tracer()

  override def beforeAll() {
    wiremock.start()
  }

  override def beforeEach() {
    wiremock.resetMappings()
  }

  override def afterAll() {
    wiremock.stop()
    actorSystem.terminate().futureValue
  }

  it should "traceCall with HttpRequest adds tracing headers" in traceContext { (span) =>
    whenReady(traceCall(HttpRequest(uri = Uri(testUrl)), "trace_1", span)) { response =>
      response.status == StatusCodes.OK
    }

    verifyTraceHeaders(span)
  }

  it should "traceCall with SignedRequest adds tracing headers" in traceContext { (span) =>
    val signedRequest = SignedRequest(UnsignedRequest(uri = java.net.URI.create(testUrl)), "", "")
    whenReady(traceCall(signedRequest, "trace_1", span)) { response =>
      response.status == StatusCodes.OK
    }

    verifyTraceHeaders(span)
  }

  private def traceContext(test: (Span) => Any): Unit = {
    val span: Span = tracer.newTrace().start()

    wiremock.stubFor(
      get(urlMatching(".*"))
        .willReturn(aResponse().withStatus(StatusCodes.OK.intValue))
    )

    test(span)
  }

  private def verifyTraceHeaders(span: Span): Unit = {
    wiremock.verify(
      getRequestedFor(urlEqualTo("/test"))
        .withHeader("X-B3-TraceId", equalTo(span.context.traceId.toString))
        .withHeader("X-B3-SpanId", new RegexPattern("-?\\d*"))
        .withHeader("X-B3-ParentSpanId", equalTo(span.context.traceId.toString))
        .withHeader("X-B3-Sampled", equalTo("true"))
    )
  }

}
