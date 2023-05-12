package com.mdsol.mauth.http4s

import cats.data.Kleisli
import cats.effect.IO
import com.mdsol.mauth.test.utils.{FakeMAuthServer, PortFinder, TestFixtures}
import com.mdsol.mauth.{AuthenticatorConfiguration, MAuthRequestSigner}
import munit.CatsEffectSuite
import org.http4s.dsl.io._
import org.http4s.implicits._
import org.http4s._
import org.http4s.client.Client

class MauthPublicKeyProviderSuite extends CatsEffectSuite {

  private val MAUTH_PORT = PortFinder.findFreePort()
  private val MAUTH_BASE_URL = s"http://localhost:$MAUTH_PORT"
  private val MAUTH_URL_PATH = "/mauth/v1"
  private val SECURITY_TOKENS_PATH = "/security_tokens/%s.json"
  def executeRequest(uuid: String, response: IO[Response[IO]]): Kleisli[IO, Request[IO], Response[IO]] =
    HttpRoutes
      .of[IO] {
        case GET -> _ / _ / _ / _ / "mauth" / "v1" / "security_tokens" / appId if appId == s"$uuid.json"                                  => response
        case GET -> Root / "mauth" / "v1" / "security_tokens" / appId if appId == s"${FakeMAuthServer.NON_EXISTING_CLIENT_APP_UUID}.json" => response
      }
      .orNotFound
  private def getMAuthConfiguration = new AuthenticatorConfiguration(MAUTH_BASE_URL, MAUTH_URL_PATH, SECURITY_TOKENS_PATH)

  test("MauthPublicKeyProvider retrieve PublicKey from MAuth Server") {
    val signer = new MAuthRequestSigner(
      FakeMAuthServer.EXISTING_CLIENT_APP_UUID,
      TestFixtures.PRIVATE_KEY_1
    )

    new MauthPublicKeyProvider[IO](
      getMAuthConfiguration,
      signer = signer,
      client = Client.fromHttpApp(executeRequest(FakeMAuthServer.EXISTING_CLIENT_APP_UUID.toString, Ok(FakeMAuthServer.mockedMauthTokenResponse())))
    ).getPublicKey(
      FakeMAuthServer.EXISTING_CLIENT_APP_UUID
    ).map(_.nonEmpty)
      .assertEquals(true)
  }

  test("fail on invalid response from MAuth Server") {
    val signer = new MAuthRequestSigner(
      FakeMAuthServer.EXISTING_CLIENT_APP_UUID,
      TestFixtures.PRIVATE_KEY_1
    )
    val mockedSigner = signer

    new MauthPublicKeyProvider[IO](
      getMAuthConfiguration,
      signer = mockedSigner,
      client = Client.fromHttpApp(executeRequest(FakeMAuthServer.NON_EXISTING_CLIENT_APP_UUID.toString, IO(Response[IO](status = Unauthorized))))
    ).getPublicKey(
      FakeMAuthServer.NON_EXISTING_CLIENT_APP_UUID
    ).map(_.nonEmpty)
      .assertEquals(false)
  }

}
