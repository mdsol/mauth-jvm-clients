package com.mdsol.mauth

import java.util.UUID

import com.mdsol.mauth.test.utils.ProtocolTestSuiteHelper
import com.mdsol.mauth.util.MAuthKeysHelper.getPrivateKeyFromString
import com.mdsol.mauth.util.{EpochTimeProvider, MAuthKeysHelper, MAuthSignatureHelper}
import com.mdsol.mauth.utils.ClientPublicKeyProvider
import org.scalamock.scalatest.MockFactory
import org.scalatest.BeforeAndAfterAll
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import scala.jdk.CollectionConverters._

class MauthProtocolSuiteSpec extends AnyFlatSpec with BeforeAndAfterAll with Matchers with MockFactory {

  val REQUEST_VALIDATION_TIMEOUT_SECONDS: Long = 300L
  val mockClientPublicKeyProvider: ClientPublicKeyProvider = mock[ClientPublicKeyProvider]
  val mockEpochTimeProvider: EpochTimeProvider = mock[EpochTimeProvider]
  val authenticatorV2: RequestAuthenticator =
    new RequestAuthenticator(mockClientPublicKeyProvider, REQUEST_VALIDATION_TIMEOUT_SECONDS, mockEpochTimeProvider, true)

  behavior of "MauthProtocolSuiteSpec"

  val signingConfig = ProtocolTestSuiteHelper.SIGNING_CONFIG
  if (signingConfig == null) {
    fail("Signing Configuration is not available.")
  }

  if (ProtocolTestSuiteHelper.PUBLIC_KEY == null) {
    fail("Public Key is not available.")
  }

  val publicKey = MAuthKeysHelper.getPublicKeyFromString(ProtocolTestSuiteHelper.PUBLIC_KEY)
  val uuid = UUID.fromString(signingConfig.getAppUuid)
  val privateKey = getPrivateKeyFromString(signingConfig.getPrivateKey)
  val mAuthSigner = new DefaultSigner(uuid, privateKey, mockEpochTimeProvider, java.util.Arrays.asList(MAuthVersion.MWSV2))

  // run the tests
  val testSpecifications = ProtocolTestSuiteHelper.TEST_SPECIFICATIONS
  testSpecifications.foreach { testSpec =>
    val authHeader = testSpec.getAuthenticationHeader
    val unsignedRequest = testSpec.getUnsignedRequest
    s"Test Case: ${testSpec.getName}" should "pass authentication" in {
      val mauthRequest = MAuthRequest.Builder.get
        .withAuthenticationHeaderValue(authHeader.getMccAuthentication)
        .withTimeHeaderValue(String.valueOf(authHeader.getMccTime))
        .withHttpMethod(unsignedRequest.getHttpVerb)
        .withResourcePath(unsignedRequest.getResourcePath)
        .withQueryParameters(unsignedRequest.getQueryString)
        .withMessagePayload(unsignedRequest.getBodyInBytes)
        .build()

      (mockEpochTimeProvider.inSeconds _: () => Long).expects().returns(signingConfig.getRequestTime.toLong + 3)
      (mockClientPublicKeyProvider.getPublicKey _)
        .expects(uuid)
        .returns(publicKey)
      authenticatorV2.authenticate(mauthRequest) shouldBe true
    }

    if (!testSpec.isAuthenticationOnly) {

      it should "correctly generate the string to sign" in {
        MAuthSignatureHelper.generateStringToSignV2(
          uuid,
          unsignedRequest.getHttpVerb,
          unsignedRequest.getResourcePath,
          unsignedRequest.getQueryString,
          unsignedRequest.getBodyInBytes,
          signingConfig.getRequestTime
        ) shouldBe testSpec.getStringToSign
      }

      it should "correctly generate the signature" in {
        MAuthSignatureHelper.encryptSignatureRSA(
          privateKey,
          testSpec.getStringToSign
        ) shouldBe testSpec.getSignature
      }

      it should "correctly generate the authentication headers" in {
        val httpVerb = unsignedRequest.getHttpVerb
        val resourcePath = unsignedRequest.getResourcePath
        val queryString = unsignedRequest.getQueryString
        val bodyInBytes = unsignedRequest.getBodyInBytes
        (mockEpochTimeProvider.inSeconds _: () => Long).expects().returns(signingConfig.getRequestTime.toLong)
        val headers: Map[String, String] =
          mAuthSigner
            .generateRequestHeaders(httpVerb, resourcePath, bodyInBytes, queryString)
            .asScala
            .toMap
        headers(MAuthRequest.MCC_AUTHENTICATION_HEADER_NAME) shouldBe authHeader.getMccAuthentication
        headers(MAuthRequest.MCC_TIME_HEADER_NAME) shouldBe authHeader.getMccTime.toString
      }
    }
  }
}
