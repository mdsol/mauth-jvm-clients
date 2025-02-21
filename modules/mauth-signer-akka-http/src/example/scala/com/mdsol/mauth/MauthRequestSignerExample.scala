package com.mdsol.mauth

import java.net.URI
import akka.actor.ActorSystem
import com.mdsol.mauth.http.HttpClient
import com.mdsol.mauth.http.Implicits._
import com.typesafe.config.ConfigFactory

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext}
import com.mdsol.mauth.models.{UnsignedRequest => NewUnsignedRequest}
/**
  * Example how to sign requests using Akka HttpClient
  * Set up the following environment variables:
  * APP_MAUTH_UUID - app uuid
  * APP_MAUTH_PRIVATE_KEY - the application private key itself, not the path
  */
object MauthRequestSignerExample {

  def main(args: Array[String]): Unit = {
    implicit val system: ActorSystem = ActorSystem()
    implicit val ec: ExecutionContext = system.dispatcher

    val configuration = new SignerConfiguration(ConfigFactory.load())
    val httpMethod = "GET"
    val uri = URI.create("https://api.mdsol.com/v1/countries")

    val signedRequest = MAuthRequestSigner(configuration).signRequest(NewUnsignedRequest(httpMethod, uri, body = Array.empty, headers = Map.empty))
    Await.result(
      HttpClient.call(signedRequest.toAkkaHttpRequest).map(response => println(s"response code: ${response._1.value}, response: ${response._3.toString}")),
      10.seconds
    )
  }
}
