package com.mdsol.mauth

import java.util.UUID

import com.mdsol.mauth.test.utils.ProtocolTestSuiteHelper
import com.mdsol.mauth.util.{EpochTimeProvider, MAuthKeysHelper}
import com.mdsol.mauth.utils.ClientPublicKeyProvider
import org.scalamock.scalatest.MockFactory
import org.scalatest.BeforeAndAfterAll
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.prop.TableDrivenPropertyChecks

class RequestAuthenticatorProtocolSuiteSpec extends AnyFlatSpec with BeforeAndAfterAll with Matchers with TableDrivenPropertyChecks with MockFactory {

  val REQUEST_VALIDATION_TIMEOUT_SECONDS: Long = 300L
  val mockClientPublicKeyProvider: ClientPublicKeyProvider = mock[ClientPublicKeyProvider]
  val mockEpochTimeProvider: EpochTimeProvider = mock[EpochTimeProvider]
  val authenticatorV2: RequestAuthenticator =
    new RequestAuthenticator(mockClientPublicKeyProvider, REQUEST_VALIDATION_TIMEOUT_SECONDS, mockEpochTimeProvider, true)

  behavior of "RequestAuthenticatorProtocolSuite"

  Option(ProtocolTestSuiteHelper.loadSigningConfig()) match {
    case Some(signingConfig) =>
      val testCases = Option(ProtocolTestSuiteHelper.getTestCases).getOrElse(Array[String]())
      testCases.foreach { testCase =>
        it should s"pass the test of $testCase" in {
          // run the tests
          val publicKey = MAuthKeysHelper.getPublicKeyFromString(ProtocolTestSuiteHelper.getPublicKey)
          val caseFile = testCase.concat("/").concat(testCase)
          Option(ProtocolTestSuiteHelper.loadAuthenticationHeader(caseFile.concat(".authz"))) match {
            case Some(authHeader) =>
              Option(ProtocolTestSuiteHelper.loadUnignedRequest(caseFile.concat(".req"))) match {
                case Some(unsignedRequest) =>
                  val bodyInBytes = ProtocolTestSuiteHelper.getTestRequestBody(unsignedRequest, testCase)
                  val mauthRequest = MAuthRequest.Builder.get
                    .withAuthenticationHeaderValue(authHeader.getMccAuthentication)
                    .withTimeHeaderValue(String.valueOf(authHeader.getMccTime))
                    .withHttpMethod(unsignedRequest.getHttpVerb)
                    .withResourcePath(unsignedRequest.getResourcePath)
                    .withQueryParameters(unsignedRequest.getQueryString)
                    .withMessagePayload(bodyInBytes)
                    .build()

                  (mockEpochTimeProvider.inSeconds _: () => Long).expects().returns(signingConfig.getRequestTime.toLong + 3)
                  (mockClientPublicKeyProvider.getPublicKey _)
                    .expects(UUID.fromString(signingConfig.getAppUuid))
                    .returns(publicKey)
                  authenticatorV2.authenticate(mauthRequest) shouldBe true
                case None =>
                  println("No unsigned request available, skip")
              }
            case None =>
              println("No Expected authentication available, skip")
          }
        }
      }
    case None =>
      println("No signing configuration available, done")
  }
}
