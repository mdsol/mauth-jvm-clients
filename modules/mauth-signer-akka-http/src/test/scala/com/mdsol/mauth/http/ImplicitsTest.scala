package com.mdsol.mauth.http

import java.net.URI

import akka.http.scaladsl.model.{ContentType, HttpEntity, MediaType, MediaTypes}
import com.google.common.net.HttpHeaders
import com.mdsol.mauth.http.Implicits._
import com.mdsol.mauth.{SignedRequest, UnsignedRequest}
import org.scalatest.{Matchers, WordSpec}

class ImplicitsTest extends WordSpec with Matchers {

  "Implicits fromSignedRequestToHttpRequest" should {

    "Generate a POST HttpRequest from a SignedRequest" in {
      val signedRequest = SignedRequest(UnsignedRequest(httpMethod = "POST", uri = new URI("/")), "", "")
      fromSignedRequestToHttpRequest(signedRequest).method.toString() should be("HttpMethod(POST)")
    }

    "Generate a GET HttpRequest from a SignedRequest when non-uppercase method specified" in {
      val signedRequest = SignedRequest(UnsignedRequest(httpMethod = "post", uri = new URI("/")), "", "")
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

    "Generate a POST HttpRequest should created a entity with the application/json content type" in {
      val signedRequest = SignedRequest(UnsignedRequest(httpMethod = "POST", uri = new URI("/"),
        headers = Map(HttpHeaders.CONTENT_TYPE -> MediaTypes.`application/json`.value)), "", "")
      fromSignedRequestToHttpRequest(signedRequest).entity.contentType should be(ContentType(MediaTypes.`application/json`))
    }
  }

}
