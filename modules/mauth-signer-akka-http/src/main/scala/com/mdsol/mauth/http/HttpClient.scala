package com.mdsol.mauth.http

import akka.NotUsed
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Sink}
import akka.util.ByteString
import com.mdsol.mauth.SignedRequest
import com.mdsol.mauth.http.Implicits._
import com.typesafe.scalalogging.LazyLogging

import scala.concurrent.{ExecutionContext, Future}

trait HttpClient extends LazyLogging {

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
  def call[T](request: HttpRequest, flow: Flow[ByteString, T, NotUsed])
             (implicit ec: ExecutionContext, system: ActorSystem, materializer: ActorMaterializer): Future[T] = {
    val start = System.currentTimeMillis()
    call(request).flatMap { response =>
      val elapsed = System.currentTimeMillis() - start
      logger.trace(s"Response type : ${response.entity.contentType}")
      logger.debug(s"Status: ${response.status}")
      logger.debug(s"Elapsed time: [$elapsed] ms")
      response.entity.dataBytes.via(flow).runWith(Sink.head[T])
    }
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
  def call[T](request: SignedRequest, flow: Flow[ByteString, T, NotUsed])
                (implicit ec: ExecutionContext, system: ActorSystem, materializer: ActorMaterializer): Future[T] = {
    call[T](fromSignedRequestToHttpRequest(request), flow)
  }

  /**
    * Raw Http Call semantics reproduced here for convenience
    *
    * @param request      The Http Request
    * @param ec           The ExecutionContext to use
    * @param system       The ActorSystem to use
    * @param materializer The ActorMaterialise used for this call
    * @return The Response
    */
  def call(request: HttpRequest)
          (implicit ec: ExecutionContext, system: ActorSystem, materializer: ActorMaterializer): Future[HttpResponse] = {
    logger.debug(s"HTTP CLIENT CALL : ${request.uri}")
    Http().singleRequest(request)
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
  def call(request: SignedRequest)
             (implicit ec: ExecutionContext, system: ActorSystem, materializer: ActorMaterializer): Future[HttpResponse] = {
    call(fromSignedRequestToHttpRequest(request))
  }

}

object HttpClient extends HttpClient
