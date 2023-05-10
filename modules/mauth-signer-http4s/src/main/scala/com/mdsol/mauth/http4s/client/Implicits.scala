package com.mdsol.mauth.http4s.client

import com.mdsol.mauth.models.SignedRequest
import org.http4s.headers.`Content-Type`
import org.http4s.{Header, Headers, MediaType, Method, ParseResult, Request, Uri, headers}
import org.typelevel.ci.CIString

import scala.annotation.nowarn
import scala.collection.immutable

object Implicits {

  implicit class NewSignedRequestOps(val signedRequest: SignedRequest) extends AnyVal {

    /** Create an akka-http request from a [[models.SignedRequest]]
     */
    def toHttp4sRequest[F[_]]: Request[F] = {
      val contentType: Option[`Content-Type`] = extractContentTypeFromHeaders(signedRequest.req.headers)
      val headersWithoutContentType: Map[String, String] = removeContentTypeFromHeaders(signedRequest.req.headers)

      val allHeaders: immutable.Seq[Header.Raw] = (headersWithoutContentType ++ signedRequest.mauthHeaders).toList
        .map { case (name, value) =>
          Header.Raw(CIString(name), value)
        }

      Request[F](
        method = Method.fromString(signedRequest.req.httpMethod).getOrElse(Method.GET),
        uri = Uri(path = Uri.Path.unsafeFromString(signedRequest.req.uri.toString)),
        body = fs2.Stream.emits(signedRequest.req.body),
        headers = Headers(allHeaders)
      ).withContentTypeOption(contentType)
    }

    private def extractContentTypeFromHeaders(requestHeaders: Map[String, String]): Option[`Content-Type`] = {
      MediaType
        .parse(requestHeaders(headers.`Content-Type`.toString))
        .fold(
          _ => None,
          res => Some(`Content-Type`(res))
        )
    }

    @nowarn("msg=.*Unused import.*") // compat import only needed for 2.12
    private def removeContentTypeFromHeaders(requestHeaders: Map[String, String]): Map[String, String] = {
      import scala.collection.compat._
      requestHeaders.view.filterKeys(_ != headers.`Content-Type`.toString).toMap
    }
  }

}
