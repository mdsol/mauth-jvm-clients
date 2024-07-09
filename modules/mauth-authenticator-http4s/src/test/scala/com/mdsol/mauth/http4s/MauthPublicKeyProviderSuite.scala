package com.mdsol.mauth.http4s

import cats.effect.{IO, Ref}
import com.github.benmanes.caffeine.cache.Caffeine
import com.mdsol.mauth.test.utils.{FakeMAuthServer, PortFinder, TestFixtures}
import com.mdsol.mauth.util.MAuthKeysHelper
import com.mdsol.mauth.{AuthenticatorConfiguration, MAuthRequestSigner}
import munit.CatsEffectSuite
import org.http4s.dsl.io._
import org.http4s.implicits._
import org.http4s._
import org.http4s.client.Client
import org.typelevel.log4cats.noop.NoOpLogger
import scalacache.caffeine.CaffeineCache
import scalacache.{Cache, Entry}

import java.security.PublicKey
import cats.implicits._
import org.typelevel.log4cats.Logger

import java.util.UUID

class MauthPublicKeyProviderSuite extends CatsEffectSuite {

  implicit val logger: Logger[IO] = NoOpLogger[IO]
  private val MAUTH_PORT = PortFinder.findFreePort()
  private val MAUTH_BASE_URL = s"http://localhost:$MAUTH_PORT"
  private val MAUTH_URL_PATH = "/mauth/v1"
  private val SECURITY_TOKENS_PATH = "/security_tokens/%s.json"

  private val cCache = Caffeine.newBuilder().build[UUID, Entry[IO[Option[PublicKey]]]]()
  implicit val caffeineCache: Cache[IO, UUID, IO[Option[PublicKey]]] = CaffeineCache[IO, UUID, IO[Option[PublicKey]]](underlying = cCache)

  override def beforeEach(context: BeforeEach): Unit = {
    super.beforeEach(context)
    cCache.invalidateAll()
  }

  val requestCounter: Ref[IO, Int] = Ref.unsafe(0)

  def executeRequest(uuid: String, response: IO[Response[IO]]): HttpApp[IO] =
    HttpRoutes
      .of[IO] {
        case GET -> Root / "mauth" / "v1" / "security_tokens" / appId if appId == s"$uuid.json" => requestCounter.update(_ + 1) *> response
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

  test("MauthPublicKeyProvider handles multiple calls without making multiple requests when the call succeeds") {
    val client = Client.fromHttpApp(executeRequest(FakeMAuthServer.EXISTING_CLIENT_APP_UUID.toString, Ok(FakeMAuthServer.mockedMauthTokenResponse())))

    val provider = new MauthPublicKeyProvider[IO](
      getMAuthConfiguration,
      signer = signer,
      client = client
    )

    for {
      _ <- requestCounter.set(0)
      _ <- List
             .fill(100)(
               provider.getPublicKey(
                 FakeMAuthServer.EXISTING_CLIENT_APP_UUID
               )
             )
             .parUnorderedSequence
      _ <- provider
             .getPublicKey(
               FakeMAuthServer.EXISTING_CLIENT_APP_UUID
             )
             .guarantee(requestCounter.get.assertEquals(1))
    } yield ()
  }

  test("MauthPublicKeyProvider handles multiple calls but will re-call the API if the request fails") {
    val client = Client.fromHttpApp(executeRequest(FakeMAuthServer.EXISTING_CLIENT_APP_UUID.toString, IO(Response[IO](status = Unauthorized))))

    val provider = new MauthPublicKeyProvider[IO](
      getMAuthConfiguration,
      signer = signer,
      client = client
    )

    for {
      _ <- requestCounter.set(0)
      _ <- List
             .fill(100)(
               provider.getPublicKey(
                 FakeMAuthServer.EXISTING_CLIENT_APP_UUID
               )
             )
             .parUnorderedSequence
      _ <- provider
             .getPublicKey(
               FakeMAuthServer.EXISTING_CLIENT_APP_UUID
             )
             .guarantee(requestCounter.get.map { count =>
               assert(count > 1)
             })
    } yield ()
  }

  test("fail on invalid response from MAuth Server") {
    runTest(
      IO(Response[IO](status = Unauthorized)),
      None
    )
  }

}
