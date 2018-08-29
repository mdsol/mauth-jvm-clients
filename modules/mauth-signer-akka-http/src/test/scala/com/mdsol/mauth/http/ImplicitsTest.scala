package com.mdsol.mauth.http

import java.net.URI

import akka.http.scaladsl.model._
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

    "Generate a POST HttpRequest should created an entity with the application/json content type" in {
      val signedRequest = SignedRequest(
        UnsignedRequest(httpMethod = "POST", uri = new URI("/"), headers = Map.empty, body = Some("Request body"),
          contentType = Some(ContentTypes.`application/json`.toString())),
        "", "")

      val entity = fromSignedRequestToHttpRequest(signedRequest).entity
      fromSignedRequestToHttpRequest(signedRequest).entity should be(
        HttpEntity(ContentTypes.`application/json`, "Request body"))
    }

    "Generate a POST HttpRequest should created an entity with plain/text when no content type specified" in {
      val signedRequest = SignedRequest(
        UnsignedRequest(httpMethod = "POST", uri = new URI("/"), headers = Map.empty, body = Some("")),
        "", "")
      fromSignedRequestToHttpRequest(signedRequest).entity.contentType should be(ContentTypes.`text/plain(UTF-8)`)
    }

    "Generate a POST HttpRequest should created an entity with plain/text when unknown content type specified" in {
      val signedRequest = SignedRequest(
        UnsignedRequest(httpMethod = "POST", uri = new URI("/"),
          headers = Map.empty, body = Some(""), contentType = Some("CUSTOMIZED CONTENT TYPE")),
        "", "")
      fromSignedRequestToHttpRequest(signedRequest).entity.contentType should be(ContentTypes.`text/plain(UTF-8)`)
    }

    "Generate a POST HttpRequest should created a entity with the binary content type" in {
      val signedRequest = SignedRequest(
        UnsignedRequest(httpMethod = "POST", uri = new URI("/"),
          headers = Map.empty, body = Some("Request body"),
          contentType = Some(ContentTypes.`application/octet-stream`.toString())),
        "", "")
      fromSignedRequestToHttpRequest(signedRequest).entity should be(
        HttpEntity(ContentTypes.`application/octet-stream`, "Request body".getBytes))
    }
  }

}
