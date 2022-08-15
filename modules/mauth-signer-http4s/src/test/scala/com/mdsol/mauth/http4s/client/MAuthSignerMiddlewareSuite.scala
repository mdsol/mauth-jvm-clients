package com.mdsol.mauth.http4s.client

import cats.effect.IO
import cats.syntax.all._
import com.mdsol.mauth.models.UnsignedRequest
import com.mdsol.mauth.{MAuthRequestSigner, MAuthVersion}
import munit.CatsEffectSuite
import org.http4s.client.Client
import org.http4s.{Headers, HttpRoutes, Request, Response, Status, Uri}
import org.http4s.dsl.io._
import com.mdsol.mauth.test.utils.TestFixtures._
import com.mdsol.mauth.util.EpochTimeProvider

import java.net.URI
import java.util.UUID


class MAuthSignerMiddlewareSuite extends CatsEffectSuite {

  private val CONST_EPOCH_TIME_PROVIDER: EpochTimeProvider = new EpochTimeProvider() { override def inSeconds(): Long = EXPECTED_TIME_HEADER_1.toLong }

  private val signerV2: MAuthRequestSigner = new MAuthRequestSigner(
    UUID.fromString(APP_UUID_1),
    PRIVATE_KEY_1,
    CONST_EPOCH_TIME_PROVIDER,
    java.util.Arrays.asList[MAuthVersion](MAuthVersion.MWSV2)
  )

  val signerV1: MAuthRequestSigner = new MAuthRequestSigner(
    UUID.fromString(APP_UUID_1),
    PRIVATE_KEY_1,
    CONST_EPOCH_TIME_PROVIDER,
    java.util.Arrays.asList[MAuthVersion](MAuthVersion.MWS)
  )


  private def route(headers: Map[String,String]) = HttpRoutes.of[IO] {
    case req @ POST -> Root / "v1" / "test" =>
      if (headers.forall(h => req.headers.headers.map(h => h.name.toString -> h.value).contains(h)))
        Response[IO](Status.Ok).pure[IO]
      else
        Response[IO](Status.InternalServerError).pure[IO]
  }.orNotFound


  test("correctly send a customized content-type header for v2") {

    val simpleNewUnsignedRequest =
      UnsignedRequest
        .fromStringBodyUtf8(
          httpMethod = "POST",
          uri = new URI(s"/v1/test"),
          body = "",
          headers = Map("Content-Type" -> "application/json")
        )

    val signedReq = signerV2.signRequest(simpleNewUnsignedRequest)

    val client = Client.fromHttpApp(route(signedReq.mauthHeaders ++ simpleNewUnsignedRequest.headers))

    val mAuthedClient = MAuthSigner(signerV2)(client)

    mAuthedClient.status(Request[IO](
      method = POST,
      uri = Uri.unsafeFromString(s"/v1/test"),
      headers = Headers(signedReq.mauthHeaders.toList ++ List("Content-Type" -> "application/json") )
    )).assertEquals(Status.Ok)
  }

  test("correctly send a customized content-type header for v1") {

    val simpleNewUnsignedRequest =
      UnsignedRequest
        .fromStringBodyUtf8(
          httpMethod = "POST",
          uri = new URI(s"/v1/test"),
          body = "",
          headers = Map("Content-Type" -> "application/json")
        )

    val signedReq = signerV1.signRequest(simpleNewUnsignedRequest)

    val client = Client.fromHttpApp(route(signedReq.mauthHeaders ++ simpleNewUnsignedRequest.headers))

    val mAuthedClient = MAuthSigner(signerV1)(client)

    mAuthedClient.status(Request[IO](
      method = POST,
      uri = Uri.unsafeFromString(s"/v1/test"),
      headers = Headers(signedReq.mauthHeaders.toList ++ List("Content-Type" -> "application/json"))
    )).assertEquals(Status.Ok)
  }
}
