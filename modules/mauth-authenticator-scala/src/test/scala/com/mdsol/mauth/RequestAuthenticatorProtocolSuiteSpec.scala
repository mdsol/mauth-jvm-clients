package com.mdsol.mauth

import java.util.UUID

import com.mdsol.mauth.test.utils.ProtocolTestSuiteHelper
import com.mdsol.mauth.util.{EpochTimeProvider, MAuthKeysHelper}
import com.mdsol.mauth.utils.ClientPublicKeyProvider
import org.scalamock.scalatest.MockFactory
import org.scalatest.BeforeAndAfterAll
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class RequestAuthenticatorProtocolSuiteSpec extends AnyFlatSpec with BeforeAndAfterAll with Matchers with MockFactory {

  val REQUEST_VALIDATION_TIMEOUT_SECONDS: Long = 300L
  val mockClientPublicKeyProvider: ClientPublicKeyProvider = mock[ClientPublicKeyProvider]
  val mockEpochTimeProvider: EpochTimeProvider = mock[EpochTimeProvider]
  val authenticatorV2: RequestAuthenticator =
    new RequestAuthenticator(mockClientPublicKeyProvider, REQUEST_VALIDATION_TIMEOUT_SECONDS, mockEpochTimeProvider, true)

  it should "pass the tests in protocol test suite for MWSV2" in {
    //noinspection ConvertibleToMethodValue
    Option(ProtocolTestSuiteHelper.loadSigningConfig()) match {
      case Some(signingConfig) =>
        Option(ProtocolTestSuiteHelper.getTestCases) match {
          case Some(testCases) =>
            val publicKey = MAuthKeysHelper.getPublicKeyFromString(ProtocolTestSuiteHelper.getPublicKey)
            for (caseName <- testCases) {
              println("Test case: " + caseName)
              val caseFile = caseName.concat("/").concat(caseName)
              Option(ProtocolTestSuiteHelper.loadAuthenticationHeader(caseFile.concat(".authz"))) match {
                case Some(authHeader) =>
                  Option(ProtocolTestSuiteHelper.loadUnignedRequest(caseFile.concat(".req"))) match {
                    case Some(unsignedRequest) =>
                      val bodyInBytes = ProtocolTestSuiteHelper.getTestRequestBody(unsignedRequest, caseName)
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
          case None =>
            println("No test case available, done")
        }
      case None =>
        println("No signing config available, done")
    }
  }
}
