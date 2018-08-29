package com.mdsol.mauth.http

import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.model.{HttpEntity, _}
import com.mdsol.mauth.SignedRequest
import com.mdsol.mauth.http.HttpVerbOps._

import scala.language.implicitConversions

object Implicits {

  private val DEFAULT_CONTENT_TYPE = ContentTypes.`text/plain(UTF-8)`

  implicit def fromSignedRequestToHttpRequest(sr: SignedRequest): HttpRequest = {
    val entityBody: String = sr.req.body match {
      case None => ""
      case Some(s: String) => s
    }

    HttpRequest(
      method = sr.req.httpMethod,
      uri = Uri(sr.req.uri.toString),
      entity = getHttpEntity(sr.req.contentType, entityBody))
      .withHeaders(mapToHeaderSequence(sr.req.headers) ++: scala.collection.immutable.Seq(
        `X-MWS-Authentication`(sr.authHeader),
        `X-MWS-Time`(sr.timeHeader)))
  }

  implicit def fromMaybeSignedRequestToMaybeHttpRequest(maybeSignedRequest: Option[SignedRequest]): Option[HttpRequest] =
    maybeSignedRequest.map(signedRequest => signedRequest)

  private def mapToHeaderSequence(headers: Map[String, String]): Seq[HttpHeader] =
    headers.map { case (k, v) => RawHeader(k, v) }.toSeq

  private def getHttpEntity(contentTypeOptional: Option[String], entityBody: String) = {
    contentTypeOptional match {
      case Some(contentType) => ContentType.parse(contentType) match {
        case Right(parsedContentType) => parsedContentType match {
          case nonBinary: ContentType.NonBinary => HttpEntity(nonBinary, entityBody)
          case binary => HttpEntity(binary, entityBody.getBytes)
        }
        case _ => HttpEntity(DEFAULT_CONTENT_TYPE, entityBody)
      }
      case None => HttpEntity(DEFAULT_CONTENT_TYPE, entityBody)
    }
  }

}
