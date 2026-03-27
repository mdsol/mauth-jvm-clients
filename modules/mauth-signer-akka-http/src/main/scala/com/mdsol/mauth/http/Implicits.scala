package com.mdsol.mauth.http

import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.model._
import com.mdsol.mauth.http.HttpVerbOps._
import com.mdsol.mauth.models.SignedRequest
import com.mdsol.mauth.models.{SignedRequest => NewSignedRequest}

import scala.collection.immutable

object Implicits {

  private val DEFAULT_CONTENT_TYPE = ContentTypes.`text/plain(UTF-8)`

  implicit def fromSignedRequestToHttpRequest(sr: SignedRequest): HttpRequest = {
    val contentType: Option[String] = extractContentTypeFromHeaders(sr.req.headers)
    val headersWithoutContentType: Map[String, String] = removeContentTypeFromHeaders(sr.req.headers)

    HttpRequest(method = sr.req.httpMethod, uri = Uri(sr.req.uri.toString), entity = getHttpEntity(contentType, sr.req.body))
      .withHeaders(
        mapToHeaderSequence(headersWithoutContentType) ++: scala.collection.immutable.Seq(
          `X-MWS-Authentication`(sr.mauthHeaders.getOrElse(`X-MWS-Authentication`.name, "")),
          `X-MWS-Time`(sr.mauthHeaders.getOrElse(`X-MWS-Time`.name, ""))
        )
      )
  }

  implicit class SignedRequestOps(val signedRequest: NewSignedRequest) extends AnyVal {

    /**
     * Create an akka-http request from a [[models.SignedRequest]]
     */
    def toAkkaHttpRequest: HttpRequest = {
      val contentType: Option[String] = extractContentTypeFromHeaders(signedRequest.req.headers)
      val headersWithoutContentType: Map[String, String] = removeContentTypeFromHeaders(signedRequest.req.headers)

      val allHeaders: immutable.Seq[RawHeader] = (headersWithoutContentType ++ signedRequest.mauthHeaders).toList
        .map { case (name, value) =>
          RawHeader(name, value)
        }

      HttpRequest(
        method = signedRequest.req.httpMethod,
        uri = Uri(signedRequest.req.uri.toString),
        entity = getHttpEntity(contentType, signedRequest.req.body),
        headers = allHeaders
      )
    }
  }

  implicit def fromMaybeSignedRequestToMaybeHttpRequest(maybeSignedRequest: Option[SignedRequest]): Option[HttpRequest] =
    maybeSignedRequest.map(signedRequest => signedRequest)

  private def mapToHeaderSequence(headers: Map[String, String]): Seq[HttpHeader] =
    headers.map { case (k, v) => RawHeader(k, v) }.toSeq

  private def extractContentTypeFromHeaders(requestHeaders: Map[String, String]): Option[String] =
    requestHeaders.get(headers.`Content-Type`.name)

  private def removeContentTypeFromHeaders(requestHeaders: Map[String, String]): Map[String, String] = {
    import scala.collection.compat._
    requestHeaders.view.filterKeys(_ != headers.`Content-Type`.name).toMap
  }

  private def getHttpEntity(contentTypeOptional: Option[String], entityBody: Array[Byte]) = {
    contentTypeOptional match {
      case Some(contentType) =>
        ContentType.parse(contentType) match {
          case Right(parsedContentType) => HttpEntity(parsedContentType, entityBody)
          case _                        => HttpEntity(DEFAULT_CONTENT_TYPE, entityBody)
        }
      case None => HttpEntity(DEFAULT_CONTENT_TYPE, entityBody)
    }
  }

}
