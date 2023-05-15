package com.mdsol.mauth.http4s

import cats.effect._
import cats.syntax.all._
import com.mdsol.mauth.MAuthRequest
import com.mdsol.mauth.exception.MAuthValidationException
import com.mdsol.mauth.scaladsl.utils.ClientPublicKeyProvider
import com.mdsol.mauth.test.utils.TestFixtures
import com.mdsol.mauth.util.{EpochTimeProvider, MAuthKeysHelper}
import munit.CatsEffectSuite
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.http4s._
import org.http4s.{HttpRoutes, Request, Response}
import org.http4s.syntax.literals._
import org.http4s.Method._

import java.security.{PublicKey, Security}
import java.util.UUID
import scala.concurrent.duration._

class MAuthMiddlewareSuite extends CatsEffectSuite {

  private val route: HttpRoutes[IO] =
    HttpRoutes.of {
      case req if req.uri.path === path"/" =>
        Response[IO](Status.Ok).withEntity("pong").pure[IO]
    }

  Security.addProvider(new BouncyCastleProvider)

  private val appUuid: UUID = UUID.fromString("2a6790ab-f6c6-45be-86fc-9e9be76ec12a")
  private val authPrefix: String = "MWS"
  private val authPrefixV2: String = "MWSV2"

  private val signature: String = "ih3xq6OvQ2/D5ktPDaZ4F6tanzdn2XGzZ+KOaFXC+YKVjNcSCfUiKB6T/552K3AmKm/" +
    "yZF4rdEOpsMZ0QkuFqEZdwQ8R3iWUwdrNPsmNXSVvF50pRAlcI77UP5" +
    "gUKV01xjZxfZ/M/vhzVn513bAgJ6CM8X4dtG20ki5xLsO35e2eZs5i9IwA/hEaKSm/" +
    "PH2pEHwxz5c9MMGtHiFgzgXGacziVn0fr2c6X5jb3cDjHnfNVX8o57kFjL5E0YOoeEK" +
    "DwHyflGhbfFNZys29jo83JCK2MQj9s+fZq5NsgmwuACRE6BnqKSPqwDWN4OK3N/iPcTwCsMKz/c5/3CEbMTlH8A=="
  val signatureV2: String =
    s"""h0MJYf5/zlX9VqqchANLr7XUln0RydMV4msZSXzLq2sbr3X+TGeJ60K9ZSlSuRrzyHbzzwuZABA
       |3P2j3l9t+gyBC1c/JSa8mldMrIXXYzp0lYLxLkghH09hm3k0pEW2la94K/Num3xgNymn6D/B9dJ1onRIgl+T+e/m4k6
       |T3apKHcV/6cJ9asm+jDjzB8OuCVWVsLZQKQbtiydUYNisYerKVxWPLs9SHNZ6GmAqq4ZCCpyEQZuMNF6cMmXgQ0Pxe9
       |X/yNA1Xc3Fakuga47lUQ6Bn7xvhkH6P+ZP0k4U7kidziXpxpkDts8fEXTpkvFX0PR7vaxjbMZzWsU413jyNsw==;""".stripMargin.replaceAll("\n", "")

  val wrongSignatureV2: String =
    """et2ht0OkDx20yWlPvOQn1jdTFaT3rS//3t+yl0VqiTgqeMae7x24/UzfD2WQ
      |Bk6o226eQVnCloRjGgq9iLqIIf1wrAFy4CjEHPVCwKOcfbpVQBJYLCyL3Ilz
      |VX6oDmV1Ghukk29mIlgmHGhfHPwGf3vMPvgCQ42GsnAKpRrQ9T4L2IWMM9gk
      |WRAFYDXE3igTM+mWBz3IRrJMLnC2440N/KFNmwh3mVCDxIx/3D4xGhhiGZwA
      |udVbIHmOG045CTSlajxWSNCbClM3nBmAzZn+wRD3DvdvHvDMiAtfVpz7rNLq
      |2rBY2KRNJmPBaAV5ss30FC146jfyg7b8I9fenyauaw==;""".stripMargin.replaceAll("\n", "")
  val wrongAuthHeaderV2: String = s"$authPrefixV2 $appUuid:$wrongSignatureV2"

  private val authHeader: String = s"$authPrefix $appUuid:$signature"
  private val authHeaderV2: String = s"$authPrefixV2 $appUuid:$signatureV2"

  private val requestValidationTimeout: Duration = 10.seconds

  private val timeHeader: Long = 1509041057L
  private val publicKey = MAuthKeysHelper.getPublicKeyFromString(TestFixtures.PUBLIC_KEY_1)

  private val client = new ClientPublicKeyProvider[IO] {

    override def getPublicKey(appUUID: UUID): IO[Option[PublicKey]] =
      if (appUUID == appUuid) {
        IO.pure(publicKey.some)
      } else IO.raiseError(new Throwable("Wrong app UUID"))
  }

  private val epochTimeProvider = new EpochTimeProvider {
    override def inSeconds(): Long = timeHeader
  }

  private implicit val authenticator: RequestAuthenticator[IO] = RequestAuthenticator(client, epochTimeProvider)

  private val service = MAuthMiddleware.httpRoutes[IO](requestValidationTimeout, authenticator)(route).orNotFound

  val authenticatorV2: RequestAuthenticator[IO] = new RequestAuthenticator(client, epochTimeProvider, v2OnlyAuthenticate = true)
  val serviceV2 =
    MAuthMiddleware.httpRoutes[IO](requestValidationTimeout, authenticatorV2)(route).orNotFound

  test("allow successfully authenticated request") {
    val res = service(
      Request[IO](GET, uri"/").withHeaders(
        MAuthRequest.X_MWS_TIME_HEADER_NAME -> timeHeader.toString,
        MAuthRequest.X_MWS_AUTHENTICATION_HEADER_NAME -> authHeader
      )
    )

    res.map(_.status).assertEquals(Status.Ok)
  }

  test("allow successfully authenticated request with both v1 and v2 headers, with V2 headers taking precedence") {
    val res = service(
      Request[IO](GET, uri"/").withHeaders(
        MAuthRequest.MCC_TIME_HEADER_NAME -> timeHeader.toString,
        MAuthRequest.MCC_AUTHENTICATION_HEADER_NAME -> authHeaderV2,
        MAuthRequest.X_MWS_TIME_HEADER_NAME -> (timeHeader - requestValidationTimeout.toSeconds - 10).toString,
        MAuthRequest.X_MWS_AUTHENTICATION_HEADER_NAME -> "invalid auth header"
      )
    )

    res.map(_.status).assertEquals(Status.Ok)
  }

  test("allow successfully authenticated request with both v1 and v2 headers, fallback to v1 if v2 failed") {
    val res = service(
      Request[IO](GET, uri"/").withHeaders(
        MAuthRequest.MCC_TIME_HEADER_NAME -> timeHeader.toString,
        MAuthRequest.MCC_AUTHENTICATION_HEADER_NAME -> wrongAuthHeaderV2,
        MAuthRequest.X_MWS_TIME_HEADER_NAME -> timeHeader.toString,
        MAuthRequest.X_MWS_AUTHENTICATION_HEADER_NAME -> authHeader
      )
    )

    res.map(_.status).assertEquals(Status.Ok)
  }

  test("reject request if validation times out") {
    val res = service(
      Request[IO](GET, uri"/").withHeaders(
        MAuthRequest.X_MWS_TIME_HEADER_NAME -> (timeHeader - requestValidationTimeout.toSeconds - 10).toString,
        MAuthRequest.X_MWS_AUTHENTICATION_HEADER_NAME -> authHeader
      )
    )

    res
      .redeem(
        {
          case e if e.isInstanceOf[MAuthValidationException] => true // TODO: fix removing the throwing of random Exceptions
          case _                                             => false // any other exception shouldn't be thrown
        },
        _ => false
      )
      .assert
  }

  test("reject if public key cannot be found") {
    val localClient = new ClientPublicKeyProvider[IO] {
      override def getPublicKey(appUUID: UUID): IO[Option[PublicKey]] =
        if (appUUID == appUuid) {
          IO.pure(none)
        } else IO.raiseError(new Throwable("Wrong app UUID"))
    }

    val localAuthenticator: RequestAuthenticator[IO] = RequestAuthenticator(localClient, epochTimeProvider)
    val localService =
      MAuthMiddleware.httpRoutes[IO](requestValidationTimeout, localAuthenticator)(route).orNotFound

    val res = localService(
      Request[IO](GET, uri"/").withHeaders(
        MAuthRequest.X_MWS_TIME_HEADER_NAME -> timeHeader.toString,
        MAuthRequest.X_MWS_AUTHENTICATION_HEADER_NAME -> authHeader
      )
    )

    res.map(_.status).assertEquals(Status.Unauthorized)
  }

  test("reject if Authentication header is missing") {
    val res = service(
      Request[IO](GET, uri"/").withHeaders(
        MAuthRequest.X_MWS_TIME_HEADER_NAME -> timeHeader.toString
      )
    )

    res.map(_.status).assertEquals(Status.Unauthorized)
  }

  test("reject if Time header is missing") {
    val res = service(
      Request[IO](GET, uri"/").withHeaders(
        MAuthRequest.X_MWS_TIME_HEADER_NAME -> timeHeader.toString
      )
    )

    res.map(_.status).assertEquals(Status.Unauthorized)
  }

  test("allow successfully authenticated request when authenticator supports v2 only with both v1 and v2 headers, with V2 headers taking precedence") {
    val res = serviceV2(
      Request[IO](GET, uri"/").withHeaders(
        MAuthRequest.MCC_TIME_HEADER_NAME -> timeHeader.toString,
        MAuthRequest.MCC_AUTHENTICATION_HEADER_NAME -> authHeaderV2,
        MAuthRequest.X_MWS_TIME_HEADER_NAME -> (timeHeader - requestValidationTimeout.toSeconds - 10).toString,
        MAuthRequest.X_MWS_AUTHENTICATION_HEADER_NAME -> "invalid auth header"
      )
    )

    res.map(_.status).assertEquals(Status.Ok)
  }

  test("allow successfully authenticated request with V2 headers") {
    val res = serviceV2(
      Request[IO](GET, uri"/").withHeaders(
        MAuthRequest.MCC_TIME_HEADER_NAME -> timeHeader.toString,
        MAuthRequest.MCC_AUTHENTICATION_HEADER_NAME -> authHeaderV2
      )
    )

    res.map(_.status).assertEquals(Status.Ok)
  }

  test("allow successfully authenticated request with V2 headers uppercase'd") {
    val res = serviceV2(
      Request[IO](GET, uri"/").withHeaders(
        MAuthRequest.MCC_TIME_HEADER_NAME -> timeHeader.toString,
        MAuthRequest.MCC_AUTHENTICATION_HEADER_NAME.toUpperCase -> authHeaderV2
      )
    )

    res.map(_.status).assertEquals(Status.Ok)
  }

  test("reject v2 request if it times out") {
    val res = serviceV2(
      Request[IO](GET, uri"/").withHeaders(
        MAuthRequest.MCC_TIME_HEADER_NAME -> (timeHeader - requestValidationTimeout.toSeconds - 10).toString,
        MAuthRequest.MCC_AUTHENTICATION_HEADER_NAME -> authHeaderV2
      )
    )

    res
      .redeem(
        {
          case e if e.isInstanceOf[MAuthValidationException] => true // TODO: fix removing the throwing of random Exceptions
          case _                                             => false // any other exception shouldn't be thrown
        },
        _ => false
      )
      .assert
  }

  test("reject v2 request if Authentication header is missing") {
    val res = serviceV2(
      Request[IO](GET, uri"/").withHeaders(
        MAuthRequest.MCC_TIME_HEADER_NAME -> timeHeader.toString
      )
    )

    res.map(_.status).assertEquals(Status.Unauthorized)
  }

  test("reject v2 request if Time header is missing") {
    val res = serviceV2(
      Request[IO](GET, uri"/").withHeaders(
        MAuthRequest.MCC_AUTHENTICATION_HEADER_NAME -> authHeaderV2
      )
    )

    res.map(_.status).assertEquals(Status.Unauthorized)
  }

  test("reject if only v1 headers provided when authenticator is v2 only") {
    val res = serviceV2(
      Request[IO](GET, uri"/").withHeaders(
        MAuthRequest.X_MWS_TIME_HEADER_NAME -> timeHeader.toString,
        MAuthRequest.X_MWS_AUTHENTICATION_HEADER_NAME -> authHeader
      )
    )

    res.map(_.status).assertEquals(Status.Unauthorized)
  }
}
