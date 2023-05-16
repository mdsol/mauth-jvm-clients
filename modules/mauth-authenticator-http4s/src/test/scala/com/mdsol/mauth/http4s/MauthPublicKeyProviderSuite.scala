package com.mdsol.mauth.http4s

import cats.effect.IO
import com.mdsol.mauth.test.utils.{FakeMAuthServer, PortFinder, TestFixtures}
import com.mdsol.mauth.util.MAuthKeysHelper
import com.mdsol.mauth.{AuthenticatorConfiguration, MAuthRequestSigner}
import munit.CatsEffectSuite
import org.http4s.dsl.io._
import org.http4s.implicits._
import org.http4s._
import org.http4s.client.Client
import org.typelevel.log4cats.noop.NoOpLogger

import java.security.PublicKey

class MauthPublicKeyProviderSuite extends CatsEffectSuite {

  implicit val logger = NoOpLogger[IO]
  private val MAUTH_PORT = PortFinder.findFreePort()
  private val MAUTH_BASE_URL = s"http://localhost:$MAUTH_PORT"
  private val MAUTH_URL_PATH = "/mauth/v1"
  private val SECURITY_TOKENS_PATH = "/security_tokens/%s.json"
  def executeRequest(uuid: String, response: IO[Response[IO]]): HttpApp[IO] =
    HttpRoutes
      .of[IO] {
        case GET -> Root / "mauth" / "v1" / "security_tokens" / appId if appId == s"$uuid.json" => response
      }
      .orNotFound

  val signer = new MAuthRequestSigner(
    FakeMAuthServer.EXISTING_CLIENT_APP_UUID,
    TestFixtures.PRIVATE_KEY_1
  )
  private def getMAuthConfiguration = new AuthenticatorConfiguration(MAUTH_BASE_URL, MAUTH_URL_PATH, SECURITY_TOKENS_PATH)

  private def runTest(response: IO[Response[IO]], assertion: Option[PublicKey]) = {
    new MauthPublicKeyProvider[IO](
      getMAuthConfiguration,
      signer = signer,
      client = Client.fromHttpApp(executeRequest(FakeMAuthServer.EXISTING_CLIENT_APP_UUID.toString, response))
    ).getPublicKey(
      FakeMAuthServer.EXISTING_CLIENT_APP_UUID
    ).assertEquals(assertion)
  }

  test("MauthPublicKeyProvider retrieve PublicKey from MAuth Server") {
    runTest(
      Ok(FakeMAuthServer.mockedMauthTokenResponse()),
      Some(MAuthKeysHelper.getPublicKeyFromString(TestFixtures.PUBLIC_KEY_1))
    )
  }

  test("fail on invalid response from MAuth Server") {
    runTest(
      IO(Response[IO](status = Unauthorized)),
      None
    )
  }

}
