package com.mdsol.mauth.http

import java.net.URI

import com.mdsol.mauth.http.Implicits._
import com.mdsol.mauth.{SignedRequest, UnsignedRequest}
import org.scalatest.{Matchers, WordSpec}

class ImplicitsTest extends WordSpec with Matchers {

  "Implicits fromSignedRequestToHttpRequest" should {

    "Generate a POST HttpRequest from a SignedRequest" in {
      val signedRequest = SignedRequest(UnsignedRequest(httpMethod = "POST", uri = new URI("/")), "X_MWS_AUTHENTICATION_HEADER_VALUE", "X_MWS_TIME_HEADER_VALUE")
      fromSignedRequestToHttpRequest(signedRequest).method.toString() should be("HttpMethod(POST)")
    }

    "Generate a GET HttpRequest from a SignedRequest when non-uppercase method specified" in {
      val signedRequest = SignedRequest(UnsignedRequest(httpMethod = "post", uri = new URI("/")), "X_MWS_AUTHENTICATION_HEADER_VALUE", "X_MWS_TIME_HEADER_VALUE")
      fromSignedRequestToHttpRequest(signedRequest).method.toString() should be("HttpMethod(GET)")
    }

    "Generate a GET HttpRequest from a SignedRequest when http method does not exist" in {
      val signedRequest = SignedRequest(UnsignedRequest(httpMethod = "INVENTED", uri = new URI("/")), "X_MWS_AUTHENTICATION_HEADER_VALUE", "X_MWS_TIME_HEADER_VALUE")
      fromSignedRequestToHttpRequest(signedRequest).method.toString() should be("HttpMethod(GET)")
    }

    "Generate a GET HttpRequest from a SignedRequest if not http method specified" in {
      val signedRequest = SignedRequest(UnsignedRequest(uri = new URI("/")), "X_MWS_AUTHENTICATION_HEADER_VALUE", "X_MWS_TIME_HEADER_VALUE")
      fromSignedRequestToHttpRequest(signedRequest).method.toString() should be("HttpMethod(GET)")
    }
  }

}
