package com.mdsol.mauth.akka.http

import java.security.Security
import java.util.UUID

import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.model.{HttpResponse, StatusCodes}
import akka.http.scaladsl.server._
import akka.http.scaladsl.testkit.ScalatestRouteTest
import com.mdsol.mauth.MAuthRequest
import com.mdsol.mauth.http.{`X-MWS-Authentication`, `X-MWS-Time`}
import com.mdsol.mauth.scaladsl.RequestAuthenticator
import com.mdsol.mauth.scaladsl.utils.ClientPublicKeyProvider
import com.mdsol.mauth.test.utils.TestFixtures
import com.mdsol.mauth.util.{EpochTimeProvider, MAuthKeysHelper}
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.scalamock.scalatest.MockFactory
import org.scalatest.Inside
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import scala.concurrent.Future
import scala.concurrent.duration._

class MAuthDirectivesSpec extends AnyWordSpec with Matchers with ScalatestRouteTest with MAuthDirectives with Directives with Inside with MockFactory {

  Security.addProvider(new BouncyCastleProvider)
  val signatureV2: String =
    s"""h0MJYf5/zlX9VqqchANLr7XUln0RydMV4msZSXzLq2sbr3X+TGeJ60K9ZSlSuRrzyHbzzwuZABA
       |3P2j3l9t+gyBC1c/JSa8mldMrIXXYzp0lYLxLkghH09hm3k0pEW2la94K/Num3xgNymn6D/B9dJ1onRIgl+T+e/m4k6
       |T3apKHcV/6cJ9asm+jDjzB8OuCVWVsLZQKQbtiydUYNisYerKVxWPLs9SHNZ6GmAqq4ZCCpyEQZuMNF6cMmXgQ0Pxe9
       |X/yNA1Xc3Fakuga47lUQ6Bn7xvhkH6P+ZP0k4U7kidziXpxpkDts8fEXTpkvFX0PR7vaxjbMZzWsU413jyNsw==;""".stripMargin.replaceAll("\n", "")
  private val signature: String = "ih3xq6OvQ2/D5ktPDaZ4F6tanzdn2XGzZ+KOaFXC+YKVjNcSCfUiKB6T/552K3AmKm/" +
    "yZF4rdEOpsMZ0QkuFqEZdwQ8R3iWUwdrNPsmNXSVvF50pRAlcI77UP5" +
    "gUKV01xjZxfZ/M/vhzVn513bAgJ6CM8X4dtG20ki5xLsO35e2eZs5i9IwA/hEaKSm/" +
    "PH2pEHwxz5c9MMGtHiFgzgXGacziVn0fr2c6X5jb3cDjHnfNVX8o57kFjL5E0YOoeEK" +
    "DwHyflGhbfFNZys29jo83JCK2MQj9s+fZq5NsgmwuACRE6BnqKSPqwDWN4OK3N/iPcTwCsMKz/c5/3CEbMTlH8A=="
  private val appUuid: UUID = UUID.fromString("2a6790ab-f6c6-45be-86fc-9e9be76ec12a")
  private val authPrefix: String = "MWS"
  private val authHeader: String = s"$authPrefix $appUuid:$signature"
  private val timeHeader: Long = 1509041057L
  private val authPrefixV2: String = "MWSV2"
  private val authHeaderV2: String = s"$authPrefixV2 $appUuid:$signatureV2"

  private implicit val timeout: FiniteDuration = 30.seconds
  private implicit val requestValidationTimeout: Duration = 10.seconds
  private val client = mock[ClientPublicKeyProvider]
  private val mockEpochTimeProvider: EpochTimeProvider = mock[EpochTimeProvider]
  private val authenticator: RequestAuthenticator = new RequestAuthenticator(client, mockEpochTimeProvider)
  private val authenticatorV2: RequestAuthenticator = new RequestAuthenticator(client, mockEpochTimeProvider, v2OnlyAuthenticate = true)

  "authenticate" should {
    lazy val route: Route = authenticate(authenticator, timeout, requestValidationTimeout).apply(complete(HttpResponse()))
    val publicKey = MAuthKeysHelper.getPublicKeyFromString(TestFixtures.PUBLIC_KEY_1)

    "pass successfully authenticated request" in {
      (client.getPublicKey _).expects(appUuid).returns(Future(Some(publicKey)))
      //noinspection ConvertibleToMethodValue
      (mockEpochTimeProvider.inSeconds _: () => Long).expects().returns(timeHeader)

      Get("/").withHeaders(
        RawHeader(MAuthRequest.X_MWS_TIME_HEADER_NAME, timeHeader.toString),
        RawHeader(MAuthRequest.X_MWS_AUTHENTICATION_HEADER_NAME, authHeader)
      ) ~> route ~> check {
        status shouldEqual StatusCodes.OK
      }
    }

    "pass successfully authenticated request with both v1 and v2 headers, with V2 headers taking precedence" in {
      (client.getPublicKey _).expects(appUuid).returns(Future(Some(publicKey)))
      (mockEpochTimeProvider.inSeconds _: () => Long).expects().returns(timeHeader)

      Get("/").withHeaders(
        RawHeader(MAuthRequest.MCC_TIME_HEADER_NAME, timeHeader.toString),
        RawHeader(MAuthRequest.MCC_AUTHENTICATION_HEADER_NAME, authHeaderV2),
        RawHeader(MAuthRequest.X_MWS_TIME_HEADER_NAME, (timeHeader - requestValidationTimeout.toSeconds - 10).toString),
        RawHeader(MAuthRequest.X_MWS_AUTHENTICATION_HEADER_NAME, "invalid auth header")
      ) ~> route ~> check {
        status shouldEqual StatusCodes.OK
      }
    }

    "pass successfully authenticated request with both v1 and v2 headers, fallback to v1 if v2 failed" in {
      (client.getPublicKey _).expects(appUuid).returns(Future(Some(publicKey)))
      (mockEpochTimeProvider.inSeconds _: () => Long).expects().returns(timeHeader)

      val wrongSignatureV2: String =
        """et2ht0OkDx20yWlPvOQn1jdTFaT3rS//3t+yl0VqiTgqeMae7x24/UzfD2WQ
          |Bk6o226eQVnCloRjGgq9iLqIIf1wrAFy4CjEHPVCwKOcfbpVQBJYLCyL3Ilz
          |VX6oDmV1Ghukk29mIlgmHGhfHPwGf3vMPvgCQ42GsnAKpRrQ9T4L2IWMM9gk
          |WRAFYDXE3igTM+mWBz3IRrJMLnC2440N/KFNmwh3mVCDxIx/3D4xGhhiGZwA
          |udVbIHmOG045CTSlajxWSNCbClM3nBmAzZn+wRD3DvdvHvDMiAtfVpz7rNLq
          |2rBY2KRNJmPBaAV5ss30FC146jfyg7b8I9fenyauaw==;""".stripMargin.replaceAll("\n", "")
      val wrongAuthHeaderV2: String = s"$authPrefixV2 $appUuid:$wrongSignatureV2"
      Get("/").withHeaders(
        RawHeader(MAuthRequest.MCC_TIME_HEADER_NAME, timeHeader.toString),
        RawHeader(MAuthRequest.MCC_AUTHENTICATION_HEADER_NAME, wrongAuthHeaderV2),
        RawHeader(MAuthRequest.X_MWS_TIME_HEADER_NAME, timeHeader.toString),
        RawHeader(MAuthRequest.X_MWS_AUTHENTICATION_HEADER_NAME, authHeader)
      ) ~> route ~> check {
        status shouldEqual StatusCodes.OK
      }
    }

    "reject if request validation timeout passed" in {
      (client.getPublicKey _).expects(*).never
      //noinspection ConvertibleToMethodValue
      (mockEpochTimeProvider.inSeconds _: () => Long).expects().returns(timeHeader)

      Get().withHeaders(
        RawHeader(MAuthRequest.X_MWS_TIME_HEADER_NAME, (timeHeader - requestValidationTimeout.toSeconds - 10).toString),
        RawHeader(MAuthRequest.X_MWS_AUTHENTICATION_HEADER_NAME, authHeader)
      ) ~> route ~> check {
        inside(rejection) { case MdsolAuthFailedRejection => }
      }
    }

    "reject if public key cannot be found" in {
      (client.getPublicKey _).expects(appUuid).returns(Future(None))
      //noinspection ConvertibleToMethodValue
      (mockEpochTimeProvider.inSeconds _: () => Long).expects().returns(timeHeader)

      Get().withHeaders(
        RawHeader(MAuthRequest.X_MWS_TIME_HEADER_NAME, timeHeader.toString),
        RawHeader(MAuthRequest.X_MWS_AUTHENTICATION_HEADER_NAME, authHeader)
      ) ~> route ~> check {
        inside(rejection) { case MdsolAuthFailedRejection => }
      }
    }

    "reject if Authentication header is missing" in {
      (client.getPublicKey _).expects(appUuid).never

      Get().withHeaders(RawHeader(MAuthRequest.X_MWS_TIME_HEADER_NAME, timeHeader.toString)) ~> route ~> check {
        inside(rejection) {
          case MissingHeaderRejection(headerName) => headerName.replaceAll("_", "-").toLowerCase shouldEqual `X-MWS-Authentication`.name
        }
      }
    }

    "reject if Time header is missing" in {
      (client.getPublicKey _).expects(appUuid).never

      Get().withHeaders(RawHeader(MAuthRequest.X_MWS_AUTHENTICATION_HEADER_NAME, authHeader)) ~> route ~> check {
        inside(rejection) {
          case MissingHeaderRejection(headerName) => headerName.replaceAll("_", "-").toLowerCase shouldEqual `X-MWS-Time`.name
        }
      }
    }
  }

  "extractMwsTimeHeader" should {
    lazy val route =
      extractMwsTimeHeader(x => complete(x.toString))

    "extract time from request" in {
      Get().withHeaders(RawHeader(MAuthRequest.X_MWS_TIME_HEADER_NAME, "1234567")) ~> route ~> check {
        responseAs[String] shouldBe "1234567"
      }
    }

    "reject with a MalformedHeaderRejection if supplied with bad format" in {
      Get().withHeaders(RawHeader(MAuthRequest.X_MWS_TIME_HEADER_NAME, "xyz")) ~> route ~> check {
        inside(rejection) { case MalformedHeaderRejection("x-mws-time", "x-mws-time header supplied with bad format: [xyz]", None) => }
      }
    }

    "reject with a MissingHeaderRejection if header is missing" in {
      Get() ~> route ~> check {
        inside(rejection) {
          case MissingHeaderRejection(headerName) => headerName.replaceAll("_", "-").toLowerCase shouldEqual `X-MWS-Time`.name
        }
      }
    }
  }

  "extractMAuthHeader" should {
    lazy val route =
      extractMAuthHeader(x => complete(x.toString))

    "extract Authentication Signature from request" in {
      Get().withHeaders(RawHeader(MAuthRequest.X_MWS_AUTHENTICATION_HEADER_NAME, authHeader)) ~> route ~> check {
        responseAs[String] shouldBe AuthHeaderDetail(appUuid, signature).toString
      }
    }

    "reject with a MalformedHeaderRejection if Authentication is missing the Prefix MWS" in {
      val wrongHeader = s" $appUuid:$signature"
      Get().withHeaders(RawHeader(MAuthRequest.X_MWS_AUTHENTICATION_HEADER_NAME, wrongHeader)) ~> route ~> check {
        inside(rejection) {
          case MalformedHeaderRejection(actualHeader, actualMsg, _) =>
            actualHeader shouldBe "x-mws-authentication"
            actualMsg shouldBe s"x-mws-authentication header supplied with bad format: [$wrongHeader]"
        }
      }
    }

    "reject with a MalformedHeaderRejection if Authentication is missing the App UUID" in {
      val wrongHeader = s"$authPrefix :$signature"
      Get().withHeaders(RawHeader(MAuthRequest.X_MWS_AUTHENTICATION_HEADER_NAME, wrongHeader)) ~> route ~> check {
        inside(rejection) {
          case MalformedHeaderRejection(actualHeader, actualMsg, _) =>
            actualHeader shouldBe "x-mws-authentication"
            actualMsg shouldBe s"x-mws-authentication header supplied with bad format: [$wrongHeader]"
        }
      }
    }

    "reject with a MalformedHeaderRejection if Authentication is missing the signature" in {
      val wrongHeader = s"$authPrefix $appUuid:"
      Get().withHeaders(RawHeader(MAuthRequest.X_MWS_AUTHENTICATION_HEADER_NAME, wrongHeader)) ~> route ~> check {
        inside(rejection) {
          case MalformedHeaderRejection(actualHeader, actualMsg, _) =>
            actualHeader shouldBe "x-mws-authentication"
            actualMsg shouldBe s"x-mws-authentication header supplied with bad format: [$wrongHeader]"
        }
      }
    }

    "reject with a MissingHeaderRejection if header is missing" in {
      Get() ~> route ~> check {
        inside(rejection) {
          case MissingHeaderRejection(headerName) => headerName.replaceAll("_", "-").toLowerCase shouldEqual `X-MWS-Authentication`.name
        }
      }
    }
  }

  "authenticate when v2OnlyAuthenticate = true" should {
    lazy val route: Route = authenticate(authenticatorV2, timeout, requestValidationTimeout).apply(complete(HttpResponse()))
    val publicKey = MAuthKeysHelper.getPublicKeyFromString(TestFixtures.PUBLIC_KEY_1)

    "pass successfully authenticated request with both v1 and v2 headers, with V2 headers taking precedence" in {
      (client.getPublicKey _).expects(appUuid).returns(Future(Some(publicKey)))
      (mockEpochTimeProvider.inSeconds _: () => Long).expects().returns(timeHeader)

      Get("/").withHeaders(
        RawHeader(MAuthRequest.MCC_TIME_HEADER_NAME, timeHeader.toString),
        RawHeader(MAuthRequest.MCC_AUTHENTICATION_HEADER_NAME, authHeaderV2),
        RawHeader(MAuthRequest.X_MWS_TIME_HEADER_NAME, (timeHeader - requestValidationTimeout.toSeconds - 10).toString),
        RawHeader(MAuthRequest.X_MWS_AUTHENTICATION_HEADER_NAME, "invalid auth header")
      ) ~> route ~> check {
        status shouldEqual StatusCodes.OK
      }
    }

    "pass successfully authenticated request with V2 headers" in {
      (client.getPublicKey _).expects(appUuid).returns(Future(Some(publicKey)))
      //noinspection ConvertibleToMethodValue
      (mockEpochTimeProvider.inSeconds _: () => Long).expects().returns(timeHeader)

      Get("/").withHeaders(
        RawHeader(MAuthRequest.MCC_TIME_HEADER_NAME, timeHeader.toString),
        RawHeader(MAuthRequest.MCC_AUTHENTICATION_HEADER_NAME, authHeaderV2)
      ) ~> route ~> check {
        status shouldEqual StatusCodes.OK
      }
    }

    "pass successfully authenticated request with V2 headers in UpperCase " in {
      (client.getPublicKey _).expects(appUuid).returns(Future(Some(publicKey)))
      //noinspection ConvertibleToMethodValue
      (mockEpochTimeProvider.inSeconds _: () => Long).expects().returns(timeHeader)

      Get("/").withHeaders(
        RawHeader(MAuthRequest.MCC_TIME_HEADER_NAME.toUpperCase(), timeHeader.toString),
        RawHeader(MAuthRequest.MCC_AUTHENTICATION_HEADER_NAME.toUpperCase(), authHeaderV2)
      ) ~> route ~> check {
        status shouldEqual StatusCodes.OK
      }
    }

    "reject if request validation timeout passed" in {
      (client.getPublicKey _).expects(*).never
      //noinspection ConvertibleToMethodValue
      (mockEpochTimeProvider.inSeconds _: () => Long).expects().returns(timeHeader)

      Get().withHeaders(
        RawHeader(MAuthRequest.MCC_TIME_HEADER_NAME, (timeHeader - requestValidationTimeout.toSeconds - 10).toString),
        RawHeader(MAuthRequest.MCC_AUTHENTICATION_HEADER_NAME, authHeaderV2),
        RawHeader(MAuthRequest.X_MWS_TIME_HEADER_NAME, timeHeader.toString),
        RawHeader(MAuthRequest.X_MWS_AUTHENTICATION_HEADER_NAME, authHeader)
      ) ~> route ~> check {
        inside(rejection) { case MdsolAuthFailedRejection => }
      }
    }

    "reject if public key cannot be found" in {
      (client.getPublicKey _).expects(appUuid).returns(Future(None))
      //noinspection ConvertibleToMethodValue
      (mockEpochTimeProvider.inSeconds _: () => Long).expects().returns(timeHeader)

      Get().withHeaders(
        RawHeader(MAuthRequest.MCC_TIME_HEADER_NAME, timeHeader.toString),
        RawHeader(MAuthRequest.MCC_AUTHENTICATION_HEADER_NAME, authHeaderV2),
        RawHeader(MAuthRequest.X_MWS_TIME_HEADER_NAME, timeHeader.toString),
        RawHeader(MAuthRequest.X_MWS_AUTHENTICATION_HEADER_NAME, authHeader)
      ) ~> route ~> check {
        inside(rejection) { case MdsolAuthFailedRejection => }
      }
    }

    "reject if Authentication header is missing" in {
      (client.getPublicKey _).expects(appUuid).never

      Get().withHeaders(RawHeader(MAuthRequest.MCC_TIME_HEADER_NAME, timeHeader.toString)) ~> route ~> check {
        inside(rejection) {
          case MissingHeaderRejection(headerName) => headerName.replaceAll("_", "-").toLowerCase shouldEqual MAuthRequest.MCC_AUTHENTICATION_HEADER_NAME
        }
      }
    }

    "reject if Time header is missing" in {
      (client.getPublicKey _).expects(appUuid).never

      Get().withHeaders(RawHeader(MAuthRequest.MCC_AUTHENTICATION_HEADER_NAME, authHeaderV2)) ~> route ~> check {
        inside(rejection) {
          case MissingHeaderRejection(headerName) => headerName.replaceAll("_", "-").toLowerCase shouldEqual MAuthRequest.MCC_TIME_HEADER_NAME
        }
      }
    }

    "reject if only v1 headers provided" in {
      (client.getPublicKey _).expects(appUuid).never

      Get().withHeaders(
        RawHeader(MAuthRequest.X_MWS_TIME_HEADER_NAME, timeHeader.toString),
        RawHeader(MAuthRequest.X_MWS_AUTHENTICATION_HEADER_NAME, authHeader)
      ) ~> route ~> check {
        inside(rejection) {
          case MissingHeaderRejection(headerName) => headerName.replaceAll("_", "-").toLowerCase shouldEqual MAuthRequest.MCC_AUTHENTICATION_HEADER_NAME
        }
      }
    }

  }

  "extractLatestAuthenticationHeaders" should {
    lazy val route =
      extractLatestAuthenticationHeaders(false)(x => complete(x.toString))

    "extract Authentication Signature from request" in {
      Get("/").withHeaders(
        RawHeader(MAuthRequest.X_MWS_TIME_HEADER_NAME, timeHeader.toString),
        RawHeader(MAuthRequest.X_MWS_AUTHENTICATION_HEADER_NAME, authHeader)
      ) ~> route ~> check {
        responseAs[String] shouldBe MauthHeaderValues(authHeader, timeHeader).toString
      }
    }

    "extract Authentication Signature from request with the both V1 and V2" in {
      Get("/").withHeaders(
        RawHeader(MAuthRequest.MCC_TIME_HEADER_NAME, timeHeader.toString),
        RawHeader(MAuthRequest.MCC_AUTHENTICATION_HEADER_NAME, authHeaderV2),
        RawHeader(MAuthRequest.X_MWS_TIME_HEADER_NAME, timeHeader.toString),
        RawHeader(MAuthRequest.X_MWS_AUTHENTICATION_HEADER_NAME, authHeader)
      ) ~> route ~> check {
        responseAs[String] shouldBe MauthHeaderValues(authHeaderV2, timeHeader).toString
      }
    }

    "reject with a MalformedHeaderRejection if Authentication header is missing" in {
      Get("/").withHeaders(
        RawHeader(MAuthRequest.X_MWS_TIME_HEADER_NAME, timeHeader.toString)
      ) ~> route ~> check {
        inside(rejection) {
          case MissingHeaderRejection(headerName) => headerName.replaceAll("_", "-").toLowerCase shouldEqual MAuthRequest.X_MWS_AUTHENTICATION_HEADER_NAME
        }
      }
    }

    "reject with a MalformedHeaderRejection if Time header is missing" in {
      Get("/").withHeaders(
        RawHeader(MAuthRequest.X_MWS_AUTHENTICATION_HEADER_NAME, authHeader)
      ) ~> route ~> check {
        inside(rejection) {
          case MissingHeaderRejection(headerName) => headerName.replaceAll("_", "-").toLowerCase shouldEqual MAuthRequest.X_MWS_TIME_HEADER_NAME
        }
      }
    }

    "reject with a MalformedHeaderRejection if V1 Time header is missing (mixed headers)" in {
      Get("/").withHeaders(
        RawHeader(MAuthRequest.X_MWS_AUTHENTICATION_HEADER_NAME, authHeader),
        RawHeader(MAuthRequest.MCC_TIME_HEADER_NAME, timeHeader.toString)
      ) ~> route ~> check {
        inside(rejection) {
          case MissingHeaderRejection(headerName) => headerName.replaceAll("_", "-").toLowerCase shouldEqual MAuthRequest.X_MWS_TIME_HEADER_NAME
        }
      }
    }

  }

  "extractLatestAuthenticationHeaders with V2 only enabled" should {
    lazy val route =
      extractLatestAuthenticationHeaders(true)(x => complete(x.toString))

    "extract Authentication Signature from request with the both V1 and V2" in {
      Get("/").withHeaders(
        RawHeader(MAuthRequest.MCC_TIME_HEADER_NAME, timeHeader.toString),
        RawHeader(MAuthRequest.MCC_AUTHENTICATION_HEADER_NAME, authHeaderV2),
        RawHeader(MAuthRequest.X_MWS_TIME_HEADER_NAME, timeHeader.toString),
        RawHeader(MAuthRequest.X_MWS_AUTHENTICATION_HEADER_NAME, authHeader)
      ) ~> route ~> check {
        responseAs[String] shouldBe MauthHeaderValues(authHeaderV2, timeHeader).toString
      }
    }

    "extract Authentication Signature from request with V2" in {
      Get("/").withHeaders(
        RawHeader(MAuthRequest.MCC_TIME_HEADER_NAME, timeHeader.toString),
        RawHeader(MAuthRequest.MCC_AUTHENTICATION_HEADER_NAME, authHeaderV2)
      ) ~> route ~> check {
        responseAs[String] shouldBe MauthHeaderValues(authHeaderV2, timeHeader).toString
      }
    }

    "reject with a MalformedHeaderRejection with V1 headers only" in {
      Get("/").withHeaders(
        RawHeader(MAuthRequest.X_MWS_TIME_HEADER_NAME, timeHeader.toString),
        RawHeader(MAuthRequest.X_MWS_AUTHENTICATION_HEADER_NAME, authHeader)
      ) ~> route ~> check {
        inside(rejection) {
          case MissingHeaderRejection(headerName) => headerName.replaceAll("_", "-").toLowerCase shouldEqual MAuthRequest.MCC_AUTHENTICATION_HEADER_NAME
        }
      }
    }

    "reject with a MalformedHeaderRejection if supplied with bad format" in {
      Get().withHeaders(
        RawHeader(MAuthRequest.MCC_AUTHENTICATION_HEADER_NAME, authHeaderV2),
        RawHeader(MAuthRequest.MCC_TIME_HEADER_NAME, "xyz")
      ) ~> route ~> check {
        inside(rejection) {
          case MalformedHeaderRejection("mcc-time", "mcc-time header supplied with bad format: [xyz]", None) =>
        }
      }
    }

    "reject with a MalformedHeaderRejection if V2 Authentication header is missing" in {
      Get("/").withHeaders(
        RawHeader(MAuthRequest.MCC_TIME_HEADER_NAME, timeHeader.toString),
        RawHeader(MAuthRequest.X_MWS_AUTHENTICATION_HEADER_NAME, authHeader)
      ) ~> route ~> check {
        inside(rejection) {
          case MissingHeaderRejection(headerName) => headerName.replaceAll("_", "-").toLowerCase shouldEqual MAuthRequest.MCC_AUTHENTICATION_HEADER_NAME
        }
      }
    }

    "reject with a MalformedHeaderRejection if Time header is missing" in {
      Get("/").withHeaders(
        RawHeader(MAuthRequest.MCC_AUTHENTICATION_HEADER_NAME, authHeaderV2)
      ) ~> route ~> check {
        inside(rejection) {
          case MissingHeaderRejection(headerName) => headerName.replaceAll("_", "-").toLowerCase shouldEqual MAuthRequest.MCC_TIME_HEADER_NAME
        }
      }
    }

    "reject with a MalformedHeaderRejection if V2 Time header is missing (mixed headers)" in {
      Get("/").withHeaders(
        RawHeader(MAuthRequest.MCC_AUTHENTICATION_HEADER_NAME, authHeaderV2),
        RawHeader(MAuthRequest.X_MWS_TIME_HEADER_NAME, timeHeader.toString)
      ) ~> route ~> check {
        inside(rejection) {
          case MissingHeaderRejection(headerName) => headerName.replaceAll("_", "-").toLowerCase shouldEqual MAuthRequest.MCC_TIME_HEADER_NAME
        }
      }
    }

  }

}
