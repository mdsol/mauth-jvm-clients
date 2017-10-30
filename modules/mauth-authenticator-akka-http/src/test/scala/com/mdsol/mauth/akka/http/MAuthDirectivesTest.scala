package com.mdsol.mauth.akka.http

import java.security.Security
import java.util.UUID

import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.model.{HttpResponse, StatusCodes}
import akka.http.scaladsl.server._
import akka.http.scaladsl.testkit.ScalatestRouteTest
import com.mdsol.mauth.MAuthRequest
import com.mdsol.mauth.http.{X_MWS_Authentication, X_MWS_Time}
import com.mdsol.mauth.scaladsl.RequestAuthenticator
import com.mdsol.mauth.scaladsl.utils.ClientPublicKeyProvider
import com.mdsol.mauth.test.utils.FixturesLoader
import com.mdsol.mauth.util.{EpochTimeProvider, MAuthKeysHelper}
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.mockito.Matchers.{eq => eqTo}
import org.mockito.Mockito.{mock, when}
import org.scalatest.{Inside, Matchers, WordSpec}

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.language.postfixOps

class MAuthDirectivesTest extends WordSpec with Matchers with ScalatestRouteTest with MAuthDirectives with Directives with Inside {

  Security.addProvider(new BouncyCastleProvider)

  private val signature: String = "ih3xq6OvQ2/D5ktPDaZ4F6tanzdn2XGzZ+KOaFXC+YKVjNcSCfUiKB6T/552K3AmKm/" +
    "yZF4rdEOpsMZ0QkuFqEZdwQ8R3iWUwdrNPsmNXSVvF50pRAlcI77UP5" +
    "gUKV01xjZxfZ/M/vhzVn513bAgJ6CM8X4dtG20ki5xLsO35e2eZs5i9IwA/hEaKSm/" +
    "PH2pEHwxz5c9MMGtHiFgzgXGacziVn0fr2c6X5jb3cDjHnfNVX8o57kFjL5E0YOoeEK" +
    "DwHyflGhbfFNZys29jo83JCK2MQj9s+fZq5NsgmwuACRE6BnqKSPqwDWN4OK3N/iPcTwCsMKz/c5/3CEbMTlH8A=="
  private val appUuid: UUID = UUID.fromString("2a6790ab-f6c6-45be-86fc-9e9be76ec12a")
  private val authPrefix: String = "MWS"
  private val authHeader: String = s"$authPrefix $appUuid:$signature"
  private val timeHeader: Long = 1509041057L

  private implicit val timeout: FiniteDuration = 10 seconds
  private implicit val requestValidationTimeout: Duration = 10 seconds
  private val client = mock(classOf[ClientPublicKeyProvider])
  private val mockEpochTimeProvider: EpochTimeProvider = mock(classOf[EpochTimeProvider])
  when(mockEpochTimeProvider.inSeconds()).thenReturn(timeHeader)
  private implicit val authenticator: RequestAuthenticator = new RequestAuthenticator(client, mockEpochTimeProvider)

  "authenticate" should {
    lazy val route: Route = authenticate.apply(complete(HttpResponse()))
    val publicKey = MAuthKeysHelper.getPublicKeyFromString(FixturesLoader.getPublicKey)

    "pass successfully authenticated request" in {
      when(client.getPublicKey(eqTo(appUuid))).thenReturn(Future(Some(publicKey)))

      Get("/").withHeaders(RawHeader(MAuthRequest.X_MWS_TIME_HEADER_NAME, timeHeader.toString), RawHeader(MAuthRequest.X_MWS_AUTHENTICATION_HEADER_NAME, authHeader)) ~> route ~> check {
        status shouldEqual StatusCodes.OK
      }
    }

    "reject if request validation timeout passed" in {
      when(client.getPublicKey(eqTo(appUuid))).thenReturn(Future(None))

      Get().withHeaders(RawHeader(MAuthRequest.X_MWS_TIME_HEADER_NAME, (timeHeader - requestValidationTimeout.toSeconds - 10).toString), RawHeader(MAuthRequest.X_MWS_AUTHENTICATION_HEADER_NAME, authHeader)) ~> route ~> check {
        inside(rejection) { case MdsolAuthFailedRejection ⇒ }
      }
    }

    "reject if public key cannot be found" in {
      when(client.getPublicKey(eqTo(appUuid))).thenReturn(Future(None))

      Get().withHeaders(RawHeader(MAuthRequest.X_MWS_TIME_HEADER_NAME, timeHeader.toString), RawHeader(MAuthRequest.X_MWS_AUTHENTICATION_HEADER_NAME, authHeader)) ~> route ~> check {
        inside(rejection) { case MdsolAuthFailedRejection ⇒ }
      }
    }

    "reject if Authentication header is missing" in {
      when(client.getPublicKey(eqTo(appUuid))).thenReturn(Future(Some(publicKey)))

      Get().withHeaders(RawHeader(MAuthRequest.X_MWS_TIME_HEADER_NAME, timeHeader.toString)) ~> route ~> check {
        inside(rejection) {
          case MissingHeaderRejection(headerName) ⇒ headerName.replaceAll("_", "-").toLowerCase shouldEqual X_MWS_Authentication.name
        }
      }
    }

    "reject if Time header is missing" in {
      when(client.getPublicKey(eqTo(appUuid))).thenReturn(Future(Some(publicKey)))

      Get().withHeaders(RawHeader(MAuthRequest.X_MWS_AUTHENTICATION_HEADER_NAME, authHeader)) ~> route ~> check {
        inside(rejection) {
          case MissingHeaderRejection(headerName) ⇒ headerName.replaceAll("_", "-").toLowerCase shouldEqual X_MWS_Time.name
        }
      }
    }
  }

  "extractMwsTimeHeader" should {
    lazy val route =
      extractMwsTimeHeader { x ⇒
        complete(x.toString)
      }

    "extract time from request" in {
      Get().withHeaders(RawHeader(MAuthRequest.X_MWS_TIME_HEADER_NAME, "1234567")) ~> route ~> check {
        responseAs[String] shouldBe "1234567"
      }
    }

    "reject with a MalformedHeaderRejection if supplied with bad format" in {
      Get().withHeaders(RawHeader(MAuthRequest.X_MWS_TIME_HEADER_NAME, "xyz")) ~> route ~> check {
        inside(rejection) { case MalformedHeaderRejection("x-mws-time", "x-mws-time header supplied with bad format: [xyz]", None) ⇒ }
      }
    }

    "reject with a MissingHeaderRejection if header is missing" in {
      Get() ~> route ~> check {
        inside(rejection) {
          case MissingHeaderRejection(headerName) ⇒ headerName.replaceAll("_", "-").toLowerCase shouldEqual X_MWS_Time.name
        }
      }
    }
  }
  "extractMAuthHeader" should {
    lazy val route =
      extractMAuthHeader { x ⇒
        complete(x.toString)
      }

    "extract Authentication Signature from request" in {
      Get().withHeaders(RawHeader(MAuthRequest.X_MWS_AUTHENTICATION_HEADER_NAME, authHeader)) ~> route ~> check {
        responseAs[String] shouldBe AuthHeaderDetail(appUuid, signature).toString
      }
    }

    "reject with a MalformedHeaderRejection if Authentication is missing the Prefix MWS" in {
      val wrongHeader = s" $appUuid:$signature"
      Get().withHeaders(RawHeader(MAuthRequest.X_MWS_AUTHENTICATION_HEADER_NAME, wrongHeader)) ~> route ~> check {
        inside(rejection) {
          case MalformedHeaderRejection(actualHeader, actualMsg, _) ⇒
            actualHeader shouldBe "x-mws-authentication"
            actualMsg shouldBe s"x-mws-authentication header supplied with bad format: [$wrongHeader]"
        }
      }
    }

    "reject with a MalformedHeaderRejection if Authentication is missing the App UUID" in {
      val wrongHeader = s"$authPrefix :$signature"
      Get().withHeaders(RawHeader(MAuthRequest.X_MWS_AUTHENTICATION_HEADER_NAME, wrongHeader)) ~> route ~> check {
        inside(rejection) {
          case MalformedHeaderRejection(actualHeader, actualMsg, _) ⇒
            actualHeader shouldBe "x-mws-authentication"
            actualMsg shouldBe s"x-mws-authentication header supplied with bad format: [$wrongHeader]"
        }
      }
    }

    "reject with a MalformedHeaderRejection if Authentication is missing the signature" in {
      val wrongHeader = s"$authPrefix $appUuid:"
      Get().withHeaders(RawHeader(MAuthRequest.X_MWS_AUTHENTICATION_HEADER_NAME, wrongHeader)) ~> route ~> check {
        inside(rejection) {
          case MalformedHeaderRejection(actualHeader, actualMsg, _) ⇒
            actualHeader shouldBe "x-mws-authentication"
            actualMsg shouldBe s"x-mws-authentication header supplied with bad format: [$wrongHeader]"
        }
      }
    }

    "reject with a MissingHeaderRejection if header is missing" in {
      Get() ~> route ~> check {
        inside(rejection) {
          case MissingHeaderRejection(headerName) ⇒ headerName.replaceAll("_", "-").toLowerCase shouldEqual X_MWS_Authentication.name
        }
      }
    }
  }
}
