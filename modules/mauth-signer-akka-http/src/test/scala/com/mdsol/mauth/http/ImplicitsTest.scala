package com.mdsol.mauth.http

import java.net.URI

import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.RawHeader
import com.mdsol.mauth.http.Implicits._
import com.mdsol.mauth.{SignedRequest, UnsignedRequest}
import org.scalatest.{Matchers, WordSpec}

class ImplicitsTest extends WordSpec with Matchers {

  "Implicits fromSignedRequestToHttpRequest" should {

    "Generate a POST HttpRequest from a SignedRequest when POST method specified" in {
      val signedRequest = SignedRequest(UnsignedRequest(httpMethod = "POST", uri = new URI("/")), "", "")
      fromSignedRequestToHttpRequest(signedRequest).method.toString() should be("HttpMethod(POST)")
    }

    "Generate a POST HttpRequest from a SignedRequest when lowercase POST method specified" in {
      val signedRequest = SignedRequest(UnsignedRequest(httpMethod = "post", uri = new URI("/")), "", "")
      fromSignedRequestToHttpRequest(signedRequest).method.toString() should be("HttpMethod(POST)")
    }

    "Generate a GET HttpRequest from a SignedRequest when GET method specified" in {
      val signedRequest = SignedRequest(UnsignedRequest(httpMethod = "get", uri = new URI("/")), "", "")
      fromSignedRequestToHttpRequest(signedRequest).method.toString() should be("HttpMethod(GET)")
    }

    "Generate a GET HttpRequest from a SignedRequest when http method does not exist" in {
      val signedRequest = SignedRequest(UnsignedRequest(httpMethod = "INVENTED", uri = new URI("/")), "", "")
      fromSignedRequestToHttpRequest(signedRequest).method.toString() should be("HttpMethod(GET)")
    }

    "Generate a GET HttpRequest from a SignedRequest if not http method specified" in {
      val signedRequest = SignedRequest(UnsignedRequest(uri = new URI("/")), "", "")
      fromSignedRequestToHttpRequest(signedRequest).method.toString() should be("HttpMethod(GET)")
    }

    "Generate a POST HttpRequest should created an entity with the application/json content type" +
      " and remove content type from headers" in {

      val headers = Map("Content-Type" -> ContentTypes.`application/json`.toString(), "custom_header" -> "custom_value")
      val signedRequest = SignedRequest(
        UnsignedRequest(httpMethod = "POST", uri = new URI("/"), headers = headers, body = Some("Request body")),
        "x-mws-authentication-value",
        "x-mws-time-value"
      )

      val request = fromSignedRequestToHttpRequest(signedRequest)
      request.entity should be(HttpEntity(ContentTypes.`application/json`, "Request body"))
      request.headers.toString() should be(
        List(
          RawHeader("custom_header", "custom_value"),
          RawHeader("x-mws-authentication", "x-mws-authentication-value"),
          RawHeader("x-mws-time", "x-mws-time-value")
        ).toString()
      )
    }

    "Generate a POST HttpRequest should created an entity with plain/text when no content type specified" in {
      val signedRequest = SignedRequest(UnsignedRequest(httpMethod = "POST", uri = new URI("/"), headers = Map.empty, body = Some("")), "", "")
      fromSignedRequestToHttpRequest(signedRequest).entity.contentType should be(ContentTypes.`text/plain(UTF-8)`)
    }

    "Generate a POST HttpRequest should created an entity with plain/text when unknown content type specified" in {

      val headers = Map("Content-Type" -> "CUSTOMIZED CONTENT TYPE")

      val signedRequest = SignedRequest(UnsignedRequest(httpMethod = "POST", uri = new URI("/"), headers = headers, body = Some("")), "", "")
      fromSignedRequestToHttpRequest(signedRequest).entity.contentType should be(ContentTypes.`text/plain(UTF-8)`)
    }

    "Generate a POST HttpRequest should created a entity with the binary content type" in {

      val headers = Map("Content-Type" -> ContentTypes.`application/octet-stream`.toString())
      val signedRequest = SignedRequest(UnsignedRequest(httpMethod = "POST", uri = new URI("/"), headers = headers, body = Some("Request body")), "", "")
      fromSignedRequestToHttpRequest(signedRequest).entity should be(HttpEntity(ContentTypes.`application/octet-stream`, "Request body".getBytes))
    }
  }

}
