package com.mdsol.mauth.http

import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.model.{HttpEntity, _}
import com.mdsol.mauth.SignedRequest
import com.mdsol.mauth.http.HttpVerbOps._
import com.mdsol.mauth.models.{UnsignedRequest => NewUnsignedRequest, SignedRequest => NewSignedRequest}

import scala.language.implicitConversions

object Implicits {

  private val DEFAULT_CONTENT_TYPE = ContentTypes.`text/plain(UTF-8)`

  implicit def fromSignedRequestToHttpRequest(sr: SignedRequest): HttpRequest = {
    val entityBody: String = sr.req.body match {
      case None => ""
      case Some(s: String) => s
    }
    val contentType: Option[String] = extractContentTypeFromHeaders(sr.req.headers)
    val headersWithoutContentType: Map[String, String] = removeContentTypeFromHeaders(sr.req.headers)

    HttpRequest(
      method = sr.req.httpMethod,
      uri = Uri(sr.req.uri.toString),
      entity = getHttpEntity(contentType, entityBody))
      .withHeaders(mapToHeaderSequence(headersWithoutContentType) ++: scala.collection.immutable.Seq(
        `X-MWS-Authentication`(sr.authHeader),
        `X-MWS-Time`(sr.timeHeader)))
  }

  implicit class NewSignedRequestOps(val signedRequest: NewSignedRequest) extends AnyVal {
    /**
      * Create an akka-http request from a [[com.mdsol.mauth.models.SignedRequest]]
      */
    def toAkkaHttpRequest: HttpRequest = {
      val allHeaders = (signedRequest.req.headers ++ signedRequest.mauthHeaders).toList
          .map { case (name, value) =>
            RawHeader(name, value)
          }

      HttpRequest(
        method = signedRequest.req.httpMethod,
        uri = Uri(signedRequest.req.uri.toString),
        entity = HttpEntity(signedRequest.req.body),
        headers = allHeaders,
      )
    }
  }

  implicit def fromMaybeSignedRequestToMaybeHttpRequest(maybeSignedRequest: Option[SignedRequest]): Option[HttpRequest] =
    maybeSignedRequest.map(signedRequest => signedRequest)

  private def mapToHeaderSequence(headers: Map[String, String]): Seq[HttpHeader] =
    headers.map { case (k, v) => RawHeader(k, v) }.toSeq

  private def extractContentTypeFromHeaders(requestHeaders: Map[String, String]): Option[String] =
    requestHeaders.get(headers.`Content-Type`.name)

  private def removeContentTypeFromHeaders(requestHeaders: Map[String, String]): Map[String, String] =
    requestHeaders.filterKeys(_ != headers.`Content-Type`.name)

  private def getHttpEntity(contentTypeOptional: Option[String], entityBody: String) = {
    contentTypeOptional match {
      case Some(contentType) =>
        ContentType.parse(contentType) match {
          case Right(parsedContentType) =>
            parsedContentType match {
              case nonBinary: ContentType.NonBinary => HttpEntity(nonBinary, entityBody)
              case binary => HttpEntity(binary, entityBody.getBytes)
            }
          case _ => HttpEntity(DEFAULT_CONTENT_TYPE, entityBody)
        }
      case None => HttpEntity(DEFAULT_CONTENT_TYPE, entityBody)
    }
  }

}
