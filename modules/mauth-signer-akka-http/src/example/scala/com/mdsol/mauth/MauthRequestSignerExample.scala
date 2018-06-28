package com.mdsol.mauth

import java.net.URI

import com.mdsol.mauth.http.HttpClient
import com.typesafe.config.ConfigFactory
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer

import scala.concurrent.{ExecutionContext, Future}

/**
  * Example how to sign requests using Akka HttpClient
  * Set up the following environment variables:
  * APP_MAUTH_UUID - app uuid
  * APP_MAUTH_PRIVATE_KEY - the application private key itself, not the path
  */
object MauthRequestSignerExample extends App {

  implicit val system: ActorSystem = ActorSystem()
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val ec: ExecutionContext = system.dispatcher

  val configuration = new SignerConfiguration(ConfigFactory.load())
  val httpMethod = "GET"
  val uri = URI.create("https://api.mdsol.com/v1/countries")

  MAuthRequestSigner(configuration).signRequest(UnsignedRequest(httpMethod, uri)) match {
    case Left(e) => Future.failed(e)
    case Right(signedRequest) => HttpClient.call(signedRequest).map { response =>
      println(s"response code: ${response._1.value}, response: ${response._3.toString}")
    }.flatMap(Future.successful(_))
  }
}
