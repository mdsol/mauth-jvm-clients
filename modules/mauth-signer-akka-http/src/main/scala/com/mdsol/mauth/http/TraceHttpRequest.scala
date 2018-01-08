package com.mdsol.mauth.http

import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.model.{HttpHeader, HttpRequest}
import brave.Span

import scala.collection.immutable.Seq

final class TraceHttpRequest(val request: HttpRequest) {
  def withTraceHeaders[T](childSpan: Span): TraceHttpRequest = {
    val traceHeaders: Seq[HttpHeader] = Map(
      TraceHeaders.TRACE_ID_NAME -> Option(childSpan.context().traceId()),
      TraceHeaders.SPAN_ID_NAME -> Option(childSpan.context().spanId()),
      TraceHeaders.PARENT_SPAN_ID_NAME -> Option(childSpan.context().parentId()),
      TraceHeaders.SAMPLED_NAME -> Option(childSpan.context().sampled())
    ).collect { case (k, Some(v)) => RawHeader(k, v.toString) }.to[Seq]

    new TraceHttpRequest(request.withHeaders(traceHeaders))
  }
}
