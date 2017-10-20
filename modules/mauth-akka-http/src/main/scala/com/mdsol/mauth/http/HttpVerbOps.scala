package com.mdsol.mauth.http

import akka.http.scaladsl.model.{HttpMethod, HttpMethods}

object HttpVerbOps {

  /**
    * Get the HTTP verb of the request. required for MAuth signing
    * @param method Akka HttpMethod
    * @return
    */
  implicit def httpVerb(method: HttpMethod): String = {
    method match {
      case HttpMethods.GET => "GET"
      case HttpMethods.CONNECT => "CONNECT"
      case HttpMethods.DELETE  => "DELETE"
      case HttpMethods.HEAD  => "HEAD"
      case HttpMethods.OPTIONS  => "OPTIONS"
      case HttpMethods.PATCH => "PATCH"
      case HttpMethods.POST  => "POST"
      case HttpMethods.PUT  => "PUT"
      case HttpMethods.TRACE  => "TRACE"
      case _ => "GET"
    }
  }

  /**
    * Get the HTTP verb of the request. required for MAuth signing
    * @param method The String value of the HTTP verb
    * @return Akka HttpMethod
    */
  implicit def httpVerb(method: String): HttpMethod = {
    method match {
      case "GET" => HttpMethods.GET
      case "CONNECT" => HttpMethods.CONNECT
      case "DELETE" => HttpMethods.DELETE
      case "HEAD" => HttpMethods.HEAD
      case "OPTIONS" => HttpMethods.OPTIONS
      case "PATCH" => HttpMethods.PATCH
      case "POST" => HttpMethods.POST
      case "PUT" => HttpMethods.PUT
      case "TRACE" => HttpMethods.TRACE
      case _ => HttpMethods.GET
    }
  }
}