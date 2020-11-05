package com.mdsol.mauth.http

import akka.http.scaladsl.model.{HttpMethod, HttpMethods}

object HttpVerbOps {

  /** Get the HTTP verb of the request. required for MAuth signing
    *
    * @param method Akka HttpMethod
    * @return
    */
  implicit def httpVerb(method: HttpMethod): String = method.value

  /** Get the HTTP verb of the request. required for MAuth signing
    * @param method The String value of the HTTP verb
    * @return Akka HttpMethod
    */
  implicit def httpVerb(method: String): HttpMethod = HttpMethods.getForKey(method.toUpperCase).getOrElse(HttpMethods.GET)

}
