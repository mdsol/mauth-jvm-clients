package com.mdsol.mauth

import java.security.Security
import java.util.UUID

import com.mdsol.mauth.test.utils.ProtocolTestSuiteHelper
import com.mdsol.mauth.util.MAuthKeysHelper.getPrivateKeyFromString
import com.mdsol.mauth.util.{EpochTimeProvider, MAuthSignatureHelper}
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.scalamock.scalatest.MockFactory
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.prop.TableDrivenPropertyChecks

import scala.collection.JavaConverters._

class SignerProtocolSuiteSpec extends AnyFlatSpec with Matchers with TableDrivenPropertyChecks with MockFactory {
  private val mockEpochTimeProvider = mock[EpochTimeProvider]

  Security.addProvider(new BouncyCastleProvider)

  behavior of "SignerProtocolSuite"

  Option(ProtocolTestSuiteHelper.loadSigningConfig()) match {
    case Some(signingConfig) =>
      val testCases = Option(ProtocolTestSuiteHelper.getTestCases).getOrElse(Array[String]())
      val uuid = UUID.fromString(signingConfig.getAppUuid)
      val privateKey = getPrivateKeyFromString(signingConfig.getPrivateKey)
      val mAuthSigner = new DefaultSigner(uuid, privateKey, mockEpochTimeProvider, true)

      testCases.foreach { testCase =>
        it should s"correctly generate the signature for $testCase" in {
          val caseFile = testCase.concat("/").concat(testCase)
          Option(ProtocolTestSuiteHelper.loadUnignedRequest(caseFile.concat(".req"))) match {
            case Some(unsignedRequest) =>
              val httpVerb = unsignedRequest.getHttpVerb
              val resourcePath = unsignedRequest.getResourcePath
              val queryString = unsignedRequest.getQueryString
              val bodyInBytes = ProtocolTestSuiteHelper.getTestRequestBody(unsignedRequest, testCase)
              val expectedStringToSign: String = ProtocolTestSuiteHelper.loadTestDataAsString(caseFile.concat(".sts"))
              if (expectedStringToSign.nonEmpty) {
                MAuthSignatureHelper.generateStringToSignV2(
                  uuid,
                  httpVerb,
                  resourcePath,
                  queryString,
                  bodyInBytes,
                  signingConfig.getRequestTime
                ) shouldBe expectedStringToSign

                val expectSignature: String = ProtocolTestSuiteHelper.loadTestDataAsString(caseFile.concat(".sig"))
                if (expectSignature.nonEmpty) {
                  MAuthSignatureHelper.encryptSignatureRSA(
                    privateKey,
                    expectedStringToSign
                  ) shouldBe expectSignature
                } else {
                  println("No Expected signature available, skip to verify generated signature")
                }
              } else {
                println("No Expected StringToSign available, skip to verify generated string-to-sign & signature")
              }

              Option(ProtocolTestSuiteHelper.loadAuthenticationHeader(caseFile.concat(".authz"))) match {
                case Some(expectedAuthHeader) =>
                  (mockEpochTimeProvider.inSeconds _: () => Long).expects().returns(signingConfig.getRequestTime.toLong)
                  val headers: Map[String, String] =
                    mAuthSigner.generateRequestHeaders(httpVerb, resourcePath, bodyInBytes, queryString).asScala.toMap
                  headers(MAuthRequest.MCC_AUTHENTICATION_HEADER_NAME) shouldBe expectedAuthHeader.getMccAuthentication
                  headers(MAuthRequest.MCC_TIME_HEADER_NAME) shouldBe expectedAuthHeader.getMccTime.toString
                case None =>
                  println("No Expected authentication available, skip to verify the generated headers")
              }
            case None =>
              println("No unsigned request available, skip")
          }
        }
      }
    case None =>
      println("No signing configuration available, done")
  }
}
