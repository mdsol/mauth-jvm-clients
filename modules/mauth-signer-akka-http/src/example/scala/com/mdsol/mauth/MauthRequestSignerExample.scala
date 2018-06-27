package com.mdsol.mauth

import java.net.URI

import com.mdsol.mauth.http.HttpClient
import com.typesafe.config.ConfigFactory
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer

import scala.concurrent.{ExecutionContext, Future}

object MauthRequestSignerExample extends App {

  implicit val system: ActorSystem = ActorSystem()
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val ec: ExecutionContext = system.dispatcher

  val configuration: SignerConfiguration = new SignerConfiguration(ConfigFactory.load())
  val httpMethod = "GET"
  val uri = URI.create("https://api.mdsol.com/v1/countries")

  MAuthRequestSigner(configuration).signRequest(UnsignedRequest(httpMethod, uri)) match {
    case Left(e) => Future.failed(e)
    case Right(signedRequest) => HttpClient.call(signedRequest).map { response =>
      val statusCode = response._1
      println(s"response code: ${statusCode.value}, response: ${response._3.toString}")
    }
  }
}
