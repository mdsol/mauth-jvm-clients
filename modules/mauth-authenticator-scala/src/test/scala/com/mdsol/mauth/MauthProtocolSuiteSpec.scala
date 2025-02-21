package com.mdsol.mauth

import java.util.UUID

import com.mdsol.mauth.test.utils.model._
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

  lazy val signingConfig: SigningConfig = ProtocolTestSuiteHelper.SIGNING_CONFIG
  if (signingConfig == null) {
    fail("Signing Configuration is not available.")
  }

  if (ProtocolTestSuiteHelper.PUBLIC_KEY == null) {
    fail("Public Key is not available.")
  }

  private val publicKey = MAuthKeysHelper.getPublicKeyFromString(ProtocolTestSuiteHelper.PUBLIC_KEY)
  private val uuid = UUID.fromString(signingConfig.getAppUuid)
  private val privateKey = getPrivateKeyFromString(signingConfig.getPrivateKey)
  val mAuthSigner = new DefaultSigner(uuid, privateKey, mockEpochTimeProvider, java.util.Arrays.asList(MAuthVersion.MWSV2))

  // run the tests
  private val testSpecifications = ProtocolTestSuiteHelper.TEST_SPECIFICATIONS
  testSpecifications.foreach { testSpec =>
    testSpec.getType match {
      case CaseType.AUTHENTICATION_ONLY =>
        val authenticationOnly = testSpec.asInstanceOf[AuthenticationOnly]
        s"Test Case: ${authenticationOnly.getName}" should "pass authentication" in {
          doAuthentication(
            authenticationOnly.getUnsignedRequest,
            authenticationOnly.getAuthenticationHeader
          ) shouldBe true
        }

      case CaseType.SIGNING_AUTHENTICATION =>
        val signingAuthentication = testSpec.asInstanceOf[SigningAuthentication]
        val authHeader = signingAuthentication.getAuthenticationHeader
        val unsignedRequest = signingAuthentication.getUnsignedRequest

        val httpVerb = unsignedRequest.getHttpVerb
        val resourcePath = unsignedRequest.getResourcePath
        val queryString = unsignedRequest.getQueryString
        val bodyInBytes = unsignedRequest.getBodyInBytes
        s"Test Case: ${signingAuthentication.getName}" should "correctly generate the string to sign" in {
          MAuthSignatureHelper.generateStringToSignV2(
            uuid,
            httpVerb,
            resourcePath,
            queryString,
            bodyInBytes,
            signingConfig.getRequestTime
          ) shouldBe signingAuthentication.getStringToSign
        }

        it should "correctly generate the signature" in {
          MAuthSignatureHelper.encryptSignatureRSA(
            privateKey,
            signingAuthentication.getStringToSign
          ) shouldBe signingAuthentication.getSignature
        }

        it should "correctly generate the authentication headers" in {
          (mockEpochTimeProvider.inSeconds _: () => Long).expects().returns(signingConfig.getRequestTime.toLong)
          val headers: Map[String, String] =
            mAuthSigner
              .generateRequestHeaders(httpVerb, resourcePath, bodyInBytes, queryString)
              .asScala
              .toMap
          headers(MAuthRequest.MCC_AUTHENTICATION_HEADER_NAME) shouldBe authHeader.getMccAuthentication
          headers(MAuthRequest.MCC_TIME_HEADER_NAME) shouldBe authHeader.getMccTime.toString
        }

        it should "pass authentication" in {
          doAuthentication(unsignedRequest, authHeader) shouldBe true
        }
    }
  }

  private def doAuthentication(unsignedRequest: UnsignedRequest, authHeader: AuthenticationHeader): Boolean = {
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
    authenticatorV2.authenticate(mauthRequest)
  }

}
