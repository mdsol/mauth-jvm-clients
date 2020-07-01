package com.mdsol.mauth

import java.security.Security
import java.util.UUID
import java.util.NoSuchElementException

import com.mdsol.mauth.exceptions.MAuthKeyException
import com.mdsol.mauth.test.utils.TestFixtures
import com.mdsol.mauth.util.EpochTimeProvider
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.scalamock.scalatest.MockFactory
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import scala.jdk.CollectionConverters._

class DefaultSignerSpec extends AnyFlatSpec with Matchers with MockFactory {
  private val TEST_EPOCH_TIME = 1424700000L
  private val testUUID = UUID.fromString("2a6790ab-f6c6-45be-86fc-9e9be76ec12a")
  private val TEST_REQUEST_BODY: String = "Request Body"
  private val TEST_REQUEST_PARAMS: String = "key2=data2&key1=data1"
  private val AUTHENTICATION_HEADER_PATTERN_V2: String = s"MWSV2 $testUUID:[^;]*;"

  private val mockEpochTimeProvider = mock[EpochTimeProvider]
  private val mAuthRequestSigner = new DefaultSigner(testUUID, TestFixtures.PRIVATE_KEY_1, mockEpochTimeProvider, SignerConfiguration.ALL_SIGN_VERSIONS)
  private val mAuthRequestSignerV2 = new DefaultSigner(testUUID, TestFixtures.PRIVATE_KEY_1, mockEpochTimeProvider, java.util.Arrays.asList(MAuthVersion.MWSV2))

  Security.addProvider(new BouncyCastleProvider)

  "DefaultSigner" should "constructor with invalid key string throws an exception" in {
    val expectedException = intercept[MAuthKeyException] {
      new DefaultSigner(testUUID, "This is not a valid key", mockEpochTimeProvider)
    }
    expectedException.getMessage shouldBe "Unable to process private key string"
  }

  it should "generated headers includes time header with correct time" in {
    //noinspection ConvertibleToMethodValue
    (mockEpochTimeProvider.inSeconds _: () => Long).expects().returns(TEST_EPOCH_TIME)

    val headers: Map[String, String] = mAuthRequestSigner.generateRequestHeaders("GET", "/", "").asScala.toMap
    headers(MAuthRequest.X_MWS_TIME_HEADER_NAME) shouldBe String.valueOf(TEST_EPOCH_TIME)
  }

  it should "generated headers includes expected authentication header" in {
    //noinspection ConvertibleToMethodValue
    (mockEpochTimeProvider.inSeconds _: () => Long).expects().returns(TEST_EPOCH_TIME)

    val headers: Map[String, String] = mAuthRequestSigner.generateRequestHeaders("GET", "/", "").asScala.toMap
    val EXPECTED_GET_AUTHENTICATION_HEADER: String =
      s"""MWS $testUUID:bXkxaWM5Src65bVPd
        |v466zC9JIy79aNfjjTczXoT01Tycxkbv/8U/7utTV+HgdJvvA1Du9wDD+l
        |0dhvRb3lmEI1LIp1A4j2rogHc13n9WdV8Q9x381Te7B9uTSdOz1k/9QRZa
        |DrmFl9GtBq4xe9xQPPF/U0cOFm4R/0OMQCYamf4/mc2PZ6t8ZOCd2gGvR7
        |0l1n9PoTTSZaULcul/oR7HFK25FPjsIQ9FkYVjJ+iwKPhrIgcZwUznNL71
        |d+V8bQ2Jr3RK+1c115rlHEy9SgLh1nW8SHP+uzZMApWEFASaLyTePbuvVU
        |DtJbziWYjVvr4m20PM2aLhMmVYcKU5T288w==""".stripMargin.replaceAll("\n", "")
    headers(MAuthRequest.X_MWS_AUTHENTICATION_HEADER_NAME) shouldBe EXPECTED_GET_AUTHENTICATION_HEADER
  }

  it should "generated headers with body includes expected authentication header" in {
    //noinspection ConvertibleToMethodValue
    (mockEpochTimeProvider.inSeconds _: () => Long).expects().returns(TEST_EPOCH_TIME)

    val headers: Map[String, String] = mAuthRequestSigner.generateRequestHeaders("POST", "/", TEST_REQUEST_BODY).asScala.toMap
    val EXPECTED_POST_AUTHENTICATION_HEADER: String =
      s"""MWS $testUUID:aDItoM9IOknNhPKH9a
        |qMguASxjBErA2KzCfiZKjCQx0LyMuNZAQ/6tZWfLZ6tI+XMTV51sxc4qiSp
        |uL6UHK9WomqhPtvSDCJ7KU3Xpoi9iJ4J3VtXu8lxKZYkrUBpV0jttbhRn1H
        |3I7VHwXCdV5ptY3WEL9u1iF3whLqUKGyYxf7WFgJmBbX/V7VIRGOW8BJjfW
        |J9pDVypVBN/VOYWLlKv9o3TTZuuEBtutuBSd6cU4oTMDnQmGkWs9fDAfdkF
        |2l/ZdmD7LFryk9vuyPJ5ur82ksJIZO61fzsEh0Klg/Qcr1E9M0dj0DtxBzw
        |4W0Oc1sXH67xKrKidr3JxuBXjv5gg==""".stripMargin.replaceAll("\n", "")
    headers(MAuthRequest.X_MWS_AUTHENTICATION_HEADER_NAME) shouldBe EXPECTED_POST_AUTHENTICATION_HEADER
  }

  "When v1 only is set" should "add mauth headers to a request for V1 only" in {
    val CLIENT_REQUEST_BINARY_APP_UUID = TestFixtures.APP_UUID_V2
    val CLIENT_REQUEST_BINARY_EPOCH_TIME: Long = TestFixtures.EPOCH_TIME.toLong
    val CLIENT_REQUEST_BINARY_PATH = TestFixtures.REQUEST_PATH_V2
    val CLIENT_REQUEST_QUERY_PARAMETERS = TestFixtures.REQUEST_QUERY_PARAMETERS_V2
    val uuid = UUID.fromString(CLIENT_REQUEST_BINARY_APP_UUID)
    val mAuthSigner = new DefaultSigner(uuid, TestFixtures.PRIVATE_KEY_2, mockEpochTimeProvider, java.util.Arrays.asList(MAuthVersion.MWS))
    (mockEpochTimeProvider.inSeconds _: () => Long).expects().returns(CLIENT_REQUEST_BINARY_EPOCH_TIME)
    val EXPECTED_AUTHENTICATION_HEADER_V1: String = s"""MWS $CLIENT_REQUEST_BINARY_APP_UUID:${TestFixtures.SIGNATURE_V1_BINARY}"""
    val headers: Map[String, String] =
      mAuthSigner.generateRequestHeaders("PUT", CLIENT_REQUEST_BINARY_PATH, TestFixtures.BINARY_FILE_BODY, CLIENT_REQUEST_QUERY_PARAMETERS).asScala.toMap
    headers.size shouldEqual 2
    headers(MAuthRequest.X_MWS_AUTHENTICATION_HEADER_NAME) shouldBe EXPECTED_AUTHENTICATION_HEADER_V1
    headers(MAuthRequest.X_MWS_TIME_HEADER_NAME) shouldBe String.valueOf(CLIENT_REQUEST_BINARY_EPOCH_TIME)

    val expectedException = intercept[NoSuchElementException] {
      headers(MAuthRequest.MCC_AUTHENTICATION_HEADER_NAME)
    }
    expectedException.getMessage shouldBe "key not found: " + MAuthRequest.MCC_AUTHENTICATION_HEADER_NAME
  }

  "When v2 only is set" should "generated headers includes time header with correct time for V2" in {
    //noinspection ConvertibleToMethodValue
    (mockEpochTimeProvider.inSeconds _: () => Long).expects().returns(TEST_EPOCH_TIME)
    val headers: Map[String, String] = mAuthRequestSignerV2.generateRequestHeaders("GET", "/", "".getBytes, "").asScala.toMap
    headers(MAuthRequest.MCC_TIME_HEADER_NAME) shouldBe String.valueOf(TEST_EPOCH_TIME)
    val expectedException = intercept[NoSuchElementException] {
      headers(MAuthRequest.X_MWS_TIME_HEADER_NAME)
    }
    expectedException.getMessage shouldBe "key not found: " + MAuthRequest.X_MWS_TIME_HEADER_NAME
  }

  it should "generated headers with body includes expected authentication header for V2 only" in {
    //noinspection ConvertibleToMethodValue
    (mockEpochTimeProvider.inSeconds _: () => Long).expects().returns(TEST_EPOCH_TIME)
    val EXPECTED_POST_AUTHENTICATION_HEADER: String =
      s"""MWSV2 $testUUID:2+fhdgr8qtZ3hrY1q8x6cLCpWs4Zry1DWfu+CFKlMKE
         |lo3B6VBn/1sJ4WThTbhYzybCW/cMWRjI24iJcUDUzI4Xd6hAN6TIOcET79b
         |TbZ261vIYAMfkBshy/e2xGRoUgabqcJeGUuaEgtFy5MWq1J/w54SxcHKYLW
         |zBQ/EKFc56GnQ2cJzx0/MGX4OelIqn2P08g8mBPKS96IL4WbKuBWeoaXgAM
         |HMWfnq2CMF1Lx4sspwHL3Vux27n3iHyLfxm2ZxchHPGX61awtAV7ZPHLlhx
         |tKVsZ29Qu3ZktDThdUeQnD4qt6LoGVkzru/ynJddeLinQB0m0ixjYRFiTr3
         |YOGg==;""".stripMargin.replaceAll("\n", "")
    val headers: Map[String, String] = mAuthRequestSignerV2.generateRequestHeaders("POST", "/", TEST_REQUEST_BODY.getBytes, "").asScala.toMap
    headers.size shouldEqual 2
    headers(MAuthRequest.MCC_AUTHENTICATION_HEADER_NAME) matches AUTHENTICATION_HEADER_PATTERN_V2
    headers(MAuthRequest.MCC_AUTHENTICATION_HEADER_NAME) shouldBe EXPECTED_POST_AUTHENTICATION_HEADER
    headers(MAuthRequest.MCC_TIME_HEADER_NAME) shouldBe String.valueOf(TEST_EPOCH_TIME)
  }

  it should "generated headers with parameters includes expected authentication header for V2 only" in {
    //noinspection ConvertibleToMethodValue
    (mockEpochTimeProvider.inSeconds _: () => Long).expects().returns(TEST_EPOCH_TIME)
    val EXPECTED_GET_AUTHENTICATION_HEADER: String =
      s"""MWSV2 $testUUID:ps23lxYX9Yl9TTg6vgsETwA+SzBeXyczRVn7wcGxtxA
         |vg6llvXJjIiqy+OuJAyD9PapPP6wBMiyIu0sDdqDOq6ugBtmVNymjj7V4S/
         |ecCjgd0OPjoLHQOejNeHj5I1MySyRAOYHGZQujL84zz3Z4TFhiI4eZNmYcf
         |twCIdrUtqTTirWgL2Src9VQATAfdCjeCVZ2/TyEcNZ5NX3ep0i/vcBYIfB9
         |593PmtO7a530axpf/ogiIWLUICG2lSVVz5Y23SxnSNC0QL5sEHvby8RETES
         |Hqu5G+5/6DiTt4W2iTEkC9BUV/OObdNNbr72hN1Z5qHo/X8qZ7NMSRPyZPA
         |xe6A==;""".stripMargin.replaceAll("\n", "")
    val headers: Map[String, String] = mAuthRequestSignerV2.generateRequestHeaders("GET", "/", "".getBytes, TEST_REQUEST_PARAMS).asScala.toMap
    headers.size shouldEqual 2
    headers(MAuthRequest.MCC_AUTHENTICATION_HEADER_NAME) matches AUTHENTICATION_HEADER_PATTERN_V2
    headers(MAuthRequest.MCC_AUTHENTICATION_HEADER_NAME) shouldBe EXPECTED_GET_AUTHENTICATION_HEADER

    val expectedException = intercept[NoSuchElementException] {
      headers(MAuthRequest.X_MWS_AUTHENTICATION_HEADER_NAME)
    }
    expectedException.getMessage shouldBe "key not found: " + MAuthRequest.X_MWS_AUTHENTICATION_HEADER_NAME
  }

  "When v1 and v2 are set" should "generated headers with body for both V1 and V2" in {
    //noinspection ConvertibleToMethodValue
    val mAuthSigner = new DefaultSigner(testUUID, TestFixtures.PRIVATE_KEY_1, mockEpochTimeProvider, SignerConfiguration.ALL_SIGN_VERSIONS)
    (mockEpochTimeProvider.inSeconds _: () => Long).expects().returns(TEST_EPOCH_TIME)
    val EXPECTED_POST_AUTHENTICATION_HEADER_V1: String =
      s"""MWS $testUUID:aDItoM9IOknNhPKH9a
         |qMguASxjBErA2KzCfiZKjCQx0LyMuNZAQ/6tZWfLZ6tI+XMTV51sxc4qiSp
         |uL6UHK9WomqhPtvSDCJ7KU3Xpoi9iJ4J3VtXu8lxKZYkrUBpV0jttbhRn1H
         |3I7VHwXCdV5ptY3WEL9u1iF3whLqUKGyYxf7WFgJmBbX/V7VIRGOW8BJjfW
         |J9pDVypVBN/VOYWLlKv9o3TTZuuEBtutuBSd6cU4oTMDnQmGkWs9fDAfdkF
         |2l/ZdmD7LFryk9vuyPJ5ur82ksJIZO61fzsEh0Klg/Qcr1E9M0dj0DtxBzw
         |4W0Oc1sXH67xKrKidr3JxuBXjv5gg==""".stripMargin.replaceAll("\n", "")
    val EXPECTED_POST_AUTHENTICATION_HEADER_V2: String =
      s"""MWSV2 $testUUID:2+fhdgr8qtZ3hrY1q8x6cLCpWs4Zry1DWfu+CFKlMKE
         |lo3B6VBn/1sJ4WThTbhYzybCW/cMWRjI24iJcUDUzI4Xd6hAN6TIOcET79b
         |TbZ261vIYAMfkBshy/e2xGRoUgabqcJeGUuaEgtFy5MWq1J/w54SxcHKYLW
         |zBQ/EKFc56GnQ2cJzx0/MGX4OelIqn2P08g8mBPKS96IL4WbKuBWeoaXgAM
         |HMWfnq2CMF1Lx4sspwHL3Vux27n3iHyLfxm2ZxchHPGX61awtAV7ZPHLlhx
         |tKVsZ29Qu3ZktDThdUeQnD4qt6LoGVkzru/ynJddeLinQB0m0ixjYRFiTr3
         |YOGg==;""".stripMargin.replaceAll("\n", "")
    val headers: Map[String, String] = mAuthSigner.generateRequestHeaders("POST", "/", TEST_REQUEST_BODY.getBytes, "").asScala.toMap
    headers(MAuthRequest.X_MWS_AUTHENTICATION_HEADER_NAME) shouldBe EXPECTED_POST_AUTHENTICATION_HEADER_V1
    headers(MAuthRequest.MCC_AUTHENTICATION_HEADER_NAME) shouldBe EXPECTED_POST_AUTHENTICATION_HEADER_V2
    headers(MAuthRequest.X_MWS_TIME_HEADER_NAME) shouldBe String.valueOf(TEST_EPOCH_TIME)
    headers(MAuthRequest.MCC_TIME_HEADER_NAME) shouldBe String.valueOf(TEST_EPOCH_TIME)
  }

  it should "generated headers for binary payload for both V1 and V2" in {
    //noinspection ConvertibleToMethodValue
    val CLIENT_REQUEST_BINARY_APP_UUID = TestFixtures.APP_UUID_V2
    val CLIENT_REQUEST_BINARY_EPOCH_TIME: Long = TestFixtures.EPOCH_TIME.toLong
    val CLIENT_REQUEST_BINARY_PATH = TestFixtures.REQUEST_PATH_V2
    val CLIENT_REQUEST_QUERY_PARAMETERS = TestFixtures.REQUEST_QUERY_PARAMETERS_V2
    val uuid = UUID.fromString(CLIENT_REQUEST_BINARY_APP_UUID)
    val mAuthSigner = new DefaultSigner(uuid, TestFixtures.PRIVATE_KEY_2, mockEpochTimeProvider, SignerConfiguration.ALL_SIGN_VERSIONS)

    (mockEpochTimeProvider.inSeconds _: () => Long).expects().returns(CLIENT_REQUEST_BINARY_EPOCH_TIME)
    val EXPECTED_AUTHENTICATION_HEADER_V1: String = s"""MWS $CLIENT_REQUEST_BINARY_APP_UUID:${TestFixtures.SIGNATURE_V1_BINARY}"""
    val EXPECTED_AUTHENTICATION_HEADER_V2: String = s"""MWSV2 $CLIENT_REQUEST_BINARY_APP_UUID:${TestFixtures.SIGNATURE_V2_BINARY};"""
    val headers: Map[String, String] =
      mAuthSigner.generateRequestHeaders("PUT", CLIENT_REQUEST_BINARY_PATH, TestFixtures.BINARY_FILE_BODY, CLIENT_REQUEST_QUERY_PARAMETERS).asScala.toMap
    headers(MAuthRequest.X_MWS_AUTHENTICATION_HEADER_NAME) shouldBe EXPECTED_AUTHENTICATION_HEADER_V1
    headers(MAuthRequest.MCC_AUTHENTICATION_HEADER_NAME) shouldBe EXPECTED_AUTHENTICATION_HEADER_V2
    headers(MAuthRequest.X_MWS_TIME_HEADER_NAME) shouldBe String.valueOf(CLIENT_REQUEST_BINARY_EPOCH_TIME)
    headers(MAuthRequest.MCC_TIME_HEADER_NAME) shouldBe String.valueOf(CLIENT_REQUEST_BINARY_EPOCH_TIME)
  }

  "SignerConfiguration" should "correctly get sign versions" in {
    val expected_sign_versions = SignerConfiguration.ALL_SIGN_VERSIONS
    var signerConfig = new SignerConfiguration(testUUID, TestFixtures.PRIVATE_KEY_1, "v1,v2")
    signerConfig.getSignVersions shouldBe expected_sign_versions
    signerConfig = new SignerConfiguration(testUUID, TestFixtures.PRIVATE_KEY_1, " V1 , V2 ")
    signerConfig.getSignVersions shouldBe expected_sign_versions
  }

  it should "get the supported sign versions only" in {
    val signerConfig = new SignerConfiguration(testUUID, TestFixtures.PRIVATE_KEY_1, "dummy, v2, v20")
    signerConfig.getSignVersions shouldBe java.util.Arrays.asList(MAuthVersion.MWSV2)

    (mockEpochTimeProvider.inSeconds _: () => Long).expects().returns(TEST_EPOCH_TIME)
    val mAuthSigner = new DefaultSigner(signerConfig.getAppUUID, signerConfig.getPrivateKey, mockEpochTimeProvider, signerConfig.getSignVersions)
    val headers: Map[String, String] = mAuthSigner.generateRequestHeaders("GET", "/", "".getBytes, "").asScala.toMap
    headers(MAuthRequest.MCC_AUTHENTICATION_HEADER_NAME) matches AUTHENTICATION_HEADER_PATTERN_V2
    headers(MAuthRequest.MCC_TIME_HEADER_NAME) shouldBe String.valueOf(TEST_EPOCH_TIME)
  }
}
