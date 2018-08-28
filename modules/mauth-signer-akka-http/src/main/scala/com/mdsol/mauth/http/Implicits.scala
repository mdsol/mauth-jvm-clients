package com.mdsol.mauth.http

import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.model._
import com.mdsol.mauth.SignedRequest
import com.mdsol.mauth.http.HttpVerbOps._

import scala.language.implicitConversions

object Implicits {

  implicit def fromSignedRequestToHttpRequest(sr: SignedRequest): HttpRequest = {
    val entityBody = sr.req.body match {
      case None => ""
      case Some(s: String) => s
    }

    HttpRequest(sr.req.httpMethod, uri = Uri(sr.req.uri.toString), entity = HttpEntity(ContentTypes.`application/json`, entityBody))
      .withHeaders(mapToHeaderSequence(sr.req.headers) ++: scala.collection.immutable.Seq(
        `X-MWS-Authentication`(sr.authHeader),
        `X-MWS-Time`(sr.timeHeader)))
  }

  implicit def fromMaybeSignedRequestToMaybeHttpRequest(maybeSignedRequest: Option[SignedRequest]): Option[HttpRequest] =
    maybeSignedRequest.map(signedRequest => signedRequest)

  private def mapToHeaderSequence(headers: Map[String, String]): Seq[HttpHeader] =
    headers.map { case (k, v) => RawHeader(k, v) }.toSeq

}
