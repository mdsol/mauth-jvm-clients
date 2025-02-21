package com.mdsol.mauth

import java.net.URI
import java.security.Security
import java.util.UUID

import org.scalatest.wordspec.AnyWordSpec
import sttp.model.Uri
import sttp.client3._
import com.mdsol.mauth.test.utils.TestFixtures._
import org.scalatest.matchers.should.Matchers
import MAuthSttpSignerSpec._
import com.mdsol.mauth.util.EpochTimeProvider
import com.mdsol.mauth.util.MAuthKeysHelper.getPrivateKeyFromString
import org.bouncycastle.jce.provider.BouncyCastleProvider

class MAuthSttpSignerSpec extends AnyWordSpec with Matchers {

  Security.addProvider(new BouncyCastleProvider)

  val emptyPathNoBodyReq = basicRequest.get(Uri(URI_EMPTY_PATH))
  val emptyPathWithSimpleBodyReq = basicRequest.get(Uri(URI_EMPTY_PATH)).body(SIMPLE_REQUEST_BODY)

  "signSttpRequest" should {
    "add authentication and time header to a request for V1" in {
      val signedReq = v1v2Signer.signSttpRequest(emptyPathNoBodyReq)
      signedReq.getV1TimeHeaderValue shouldBe Some(EXPECTED_TIME_HEADER_1)
      signedReq.getV1AuthHeaderValue shouldBe Some(EXPECTED_AUTH_NO_BODY_V1)

      signedReq.getV2TimeHeaderValue shouldBe Some(EXPECTED_TIME_HEADER_1)
      signedReq.getV2AuthHeaderValue shouldBe Some(EXPECTED_AUTH_NO_BODY_V2)
    }

    "add authentication header to a request with body for V1 and V2" in {
      val signedReq = v1v2Signer.signSttpRequest(emptyPathWithSimpleBodyReq)
      signedReq.getV1AuthHeaderValue shouldBe Some(EXPECTED_AUTH_SIMPLE_BODY_V1)
      signedReq.getV2AuthHeaderValue shouldBe Some(EXPECTED_AUTH_SIMPLE_BODY_V2)
    }

    "add authentication header to a request with body and params" in {
      val req = basicRequest.get(Uri(URI_EMPTY_PATH_WITH_PARAM)).body(SIMPLE_REQUEST_BODY)
      val signedReq = v1v2Signer.signSttpRequest(req)
      signedReq.getV1AuthHeaderValue shouldBe Some(EXPECTED_AUTH_BODY_AND_PARAM_V1)
      signedReq.getV2AuthHeaderValue shouldBe Some(EXPECTED_AUTH_BODY_AND_PARAM_V2)
    }

    "When v2 only is set" should {
      "add v2 uth and time header to the request (no v1 headers)" in {
        val signedReq = v2OnlySigner.signSttpRequest(emptyPathNoBodyReq)
        signedReq.getV1TimeHeaderValue shouldBe None
        signedReq.getV1AuthHeaderValue shouldBe None

        signedReq.getV2TimeHeaderValue shouldBe Some(EXPECTED_TIME_HEADER_1)
        signedReq.getV2AuthHeaderValue shouldBe Some(EXPECTED_AUTH_NO_BODY_V2)
      }

      "add authentication header to a request with body and params for V2 only" in {
        val signedReq = v2OnlySigner.signSttpRequest(emptyPathWithSimpleBodyReq)
        signedReq.getV1TimeHeaderValue shouldBe None
        signedReq.getV1AuthHeaderValue shouldBe None

        signedReq.getV2TimeHeaderValue shouldBe Some(EXPECTED_TIME_HEADER_1)
        signedReq.getV2AuthHeaderValue shouldBe Some(EXPECTED_AUTH_SIMPLE_BODY_V2)
      }

      "add authentication header to a request for V2 with the encoded-normalize path" in {
        import sttp.client3._

        val epochTimeProvider: EpochTimeProvider = () => EPOCH_TIME.toLong

        val signer = new MAuthSttpSignerImpl(
          appUuid = UUID.fromString(APP_UUID_V2),
          privateKey = getPrivateKeyFromString(PRIVATE_KEY_2),
          epochTimeProvider = epochTimeProvider,
          signVersions = java.util.Arrays.asList(MAuthVersion.MWSV2)
        )
        val req = basicRequest.get(Uri(new URI(s"http://host.com$REQUEST_NORMALIZE_PATH"))).body("")
        val signedReq = signer.signSttpRequest(req)

        signedReq.getV2TimeHeaderValue shouldBe Some(EPOCH_TIME)
        signedReq.getV2AuthHeaderValue shouldBe Some(s"""MWSV2 $APP_UUID_V2:$SIGNATURE_NORMALIZE_PATH_V2;""")
      }

    }

    "When v1 only is set" should {
      "add v1 authentication and time header only to the request (no v2 headers)" in {
        val signedReq = v1OnlySigner.signSttpRequest(emptyPathNoBodyReq)
        signedReq.getV1TimeHeaderValue shouldBe Some(EXPECTED_TIME_HEADER_1)
        signedReq.getV1AuthHeaderValue shouldBe Some(EXPECTED_AUTH_NO_BODY_V1)

        signedReq.getV2TimeHeaderValue shouldBe None
        signedReq.getV2AuthHeaderValue shouldBe None
      }
    }

  }

  lazy val CONST_EPOCH_TIME_PROVIDER: EpochTimeProvider = new EpochTimeProvider() { override def inSeconds(): Long = EXPECTED_TIME_HEADER_1.toLong }

  lazy val v1v2Signer = new MAuthSttpSignerImpl(
    appUuid = UUID.fromString(APP_UUID_1),
    privateKey = getPrivateKeyFromString(PRIVATE_KEY_1),
    epochTimeProvider = CONST_EPOCH_TIME_PROVIDER,
    signVersions = SignerConfiguration.ALL_SIGN_VERSIONS
  )

  lazy val v2OnlySigner = new MAuthSttpSignerImpl(
    appUuid = UUID.fromString(APP_UUID_1),
    privateKey = getPrivateKeyFromString(PRIVATE_KEY_1),
    epochTimeProvider = CONST_EPOCH_TIME_PROVIDER,
    signVersions = java.util.Arrays.asList(MAuthVersion.MWSV2)
  )

  lazy val v1OnlySigner = new MAuthSttpSignerImpl(
    appUuid = UUID.fromString(APP_UUID_1),
    privateKey = getPrivateKeyFromString(PRIVATE_KEY_1),
    epochTimeProvider = CONST_EPOCH_TIME_PROVIDER,
    signVersions = java.util.Arrays.asList(MAuthVersion.MWS)
  )

}

object MAuthSttpSignerSpec {
  implicit class SttpRequestExtensions[T](val req: Request[T, Any]) extends AnyVal {
    def getHeaderValue(str: String): Option[String] =
      req.headers.find(_.is(str)).map(_.value)

    def getV1TimeHeaderValue: Option[String] = getHeaderValue(TIME_HEADER_V1)
    def getV2TimeHeaderValue: Option[String] = getHeaderValue(TIME_HEADER_V2)
    def getV1AuthHeaderValue: Option[String] = getHeaderValue(AUTH_HEADER_V1)
    def getV2AuthHeaderValue: Option[String] = getHeaderValue(AUTH_HEADER_V2)
  }
}
