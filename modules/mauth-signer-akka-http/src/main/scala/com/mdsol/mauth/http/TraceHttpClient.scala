package com.mdsol.mauth.http

import akka.NotUsed
import akka.actor.ActorSystem
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Flow
import akka.util.ByteString
import brave.{Span, Tracer}
import com.mdsol.mauth.SignedRequest
import com.mdsol.mauth.http.Implicits._

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Failure

trait TraceHttpClient extends HttpClient {

  implicit val tracer: Tracer

  /**
    * Generic call using Akka Http using the supplied HttpRequest
    *
    * @param request      The HttpRequest to satisfy
    * @param flow         The Stream Flow that determines the response processing and return type of the request
    * @param ec           The ExecutionContext to use
    * @param system       The ActorSystem to use
    * @param materializer The ActorMaterialise used for this call
    * @tparam T The type of the returned Future, based on the Stream Flow impl
    * @return The desired object
    */
  def traceCall[T](request: HttpRequest, flow: Flow[ByteString, T, NotUsed], traceName: String, parentSpan: Span)
                      (implicit ec: ExecutionContext, system: ActorSystem, materializer: ActorMaterializer): Future[T] = {
    val childSpan = tracer.newChild(parentSpan.context()).name(traceName).kind(Span.Kind.CLIENT)
    childSpan.start()

    val result = super.call(request.withTraceHeaders(childSpan), flow)

    result.onComplete {
      case Failure(t) =>
        childSpan.tag("failed", s"Finished with exception: ${t.getMessage}")
        childSpan.finish()
      case _ =>
        childSpan.finish()
    }

    result
  }

  /**
    * Generic call using Akka Http using the supplied HttpRequest
    *
    * @param request      The Signed HttpRequest to satisfy
    * @param flow         The Stream Flow that determines the response processing and return type of the request
    * @param ec           The ExecutionContext to use
    * @param system       The ActorSystem to use
    * @param materializer The ActorMaterialise used for this call
    * @tparam T The type of the returned Future, based on the Stream Flow impl
    * @return The desired object
    */
  def traceCall[T](request: SignedRequest, flow: Flow[ByteString, T, NotUsed], traceName: String, parentSpan: Span)
                      (implicit ec: ExecutionContext, system: ActorSystem, materializer: ActorMaterializer): Future[T] =
    traceCall[T](fromSignedRequestToHttpRequest(request), flow, traceName, parentSpan)

  /**
    * Raw Http Call semantics reproduced here for convenience
    *
    * @param request      The Http Request
    * @param ec           The ExecutionContext to use
    * @param system       The ActorSystem to use
    * @param materializer The ActorMaterialise used for this call
    * @return The Response
    */
  def traceCall(request: HttpRequest, traceName: String, parentSpan: Span)
                   (implicit ec: ExecutionContext, system: ActorSystem, materializer: ActorMaterializer): Future[HttpResponse] = {
    val childSpan = tracer.newChild(parentSpan.context()).name(traceName).kind(Span.Kind.CLIENT)
    childSpan.start()

    val result = super.call(request.withTraceHeaders(childSpan))

    result.onComplete {
      case Failure(t) =>
        childSpan.tag("failed", s"Finished with exception: ${t.getMessage}")
        childSpan.finish()
      case _ =>
        childSpan.finish()
    }

    result
  }

  /**
    *
    *
    * @param request      The Signed Http Request
    * @param ec           The ExecutionContext to use
    * @param system       The ActorSystem to use
    * @param materializer The ActorMaterialise used for this call
    * @return The Response
    */
  def traceCall(request: SignedRequest, traceName: String, parentSpan: Span)
                   (implicit ec: ExecutionContext, system: ActorSystem, materializer: ActorMaterializer): Future[HttpResponse] =
    traceCall(fromSignedRequestToHttpRequest(request), traceName, parentSpan)

}
