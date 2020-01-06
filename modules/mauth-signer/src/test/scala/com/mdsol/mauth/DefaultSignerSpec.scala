package com.mdsol.mauth

import java.security.Security
import java.util.UUID

import com.mdsol.mauth.exceptions.MAuthKeyException
import com.mdsol.mauth.test.utils.FixturesLoader
import com.mdsol.mauth.util.EpochTimeProvider
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.scalamock.scalatest.MockFactory
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import scala.collection.JavaConverters._

class DefaultSignerSpec extends AnyFlatSpec with Matchers with MockFactory {
  private val TEST_EPOCH_TIME = 1424700000L
  private val testUUID = UUID.fromString("2a6790ab-f6c6-45be-86fc-9e9be76ec12a")
  private val TEST_REQUEST_BODY: String = "Request Body"
  private val TEST_REQUEST_PARAMS: String = "key2=data2&key1=data1"
  private val AUTHENTICATION_HEADER_PATTERN_V2: String = "MWSV2 $testUUID:[^;]*;"

  private val mockEpochTimeProvider = mock[EpochTimeProvider]
  private val mAuthRequestSigner = new DefaultSigner(testUUID, FixturesLoader.getPrivateKey, mockEpochTimeProvider)
  private val mAuthRequestSignerV2 = new DefaultSigner(testUUID, FixturesLoader.getPrivateKey, mockEpochTimeProvider, true)

  Security.addProvider(new BouncyCastleProvider)

  it should "constructor with invalid key string throws an exception" in {
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

  it should "generated headers includes time header with correct time for V2" in {
    //noinspection ConvertibleToMethodValue
    (mockEpochTimeProvider.inSeconds _: () => Long).expects().returns(TEST_EPOCH_TIME)
    val headers: Map[String, String] = mAuthRequestSignerV2.generateRequestHeaders("GET", "/", "".getBytes, "").asScala.toMap
    headers(MAuthRequest.MCC_TIME_HEADER_NAME) shouldBe String.valueOf(TEST_EPOCH_TIME)
  }

  it should "generated headers with body includes expected authentication header for V2 only if V2 only enabled" in {
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
    headers(MAuthRequest.MCC_AUTHENTICATION_HEADER_NAME) matches (AUTHENTICATION_HEADER_PATTERN_V2)
    headers(MAuthRequest.MCC_AUTHENTICATION_HEADER_NAME) shouldBe EXPECTED_POST_AUTHENTICATION_HEADER
  }

  it should "generated headers with parameters includes expected authentication header for V2 only if V2 only enabled" in {
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
    headers(MAuthRequest.MCC_AUTHENTICATION_HEADER_NAME) matches (AUTHENTICATION_HEADER_PATTERN_V2)
    headers(MAuthRequest.MCC_AUTHENTICATION_HEADER_NAME) shouldBe EXPECTED_GET_AUTHENTICATION_HEADER
  }

  it should "generated headers for both V1 and V2 if V2 only is disabled" in {
    //noinspection ConvertibleToMethodValue
    val mAuthSigner = new DefaultSigner(testUUID, FixturesLoader.getPrivateKey, mockEpochTimeProvider, false)
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

  it should "generated headers for binary payload for both V1 and V2 if V2 only is disabled" in {
    //noinspection ConvertibleToMethodValue
    val CLIENT_REQUEST_BINARY_APP_UUID = "5ff4257e-9c16-11e0-b048-0026bbfffe5e"
    val CLIENT_REQUEST_BINARY_EPOCH_TIME = 1309891855
    val CLIENT_REQUEST_BINARY_PATH = "/v1/pictures"
    val CLIENT_REQUEST_QUERY_PARAMETERS = "key=-_.~ !@#$%^*()+{}|:\"'`<>?&∞=v&キ=v&0=v&a=v&a=b&a=c&a=a&k=&k=v"
    val mAuthSigner = new DefaultSigner(UUID.fromString(CLIENT_REQUEST_BINARY_APP_UUID), FixturesLoader.getPrivateKey2, mockEpochTimeProvider, false)
    (mockEpochTimeProvider.inSeconds _: () => Long).expects().returns(CLIENT_REQUEST_BINARY_EPOCH_TIME)
    val EXPECTED_AUTHENTICATION_HEADER_V1: String =
      s"""MWS $CLIENT_REQUEST_BINARY_APP_UUID:hDKYDRnzPFL2gzsru4zn7c7E7
         |KpEvexeF4F5IR+puDxYXrMmuT2/fETZty5NkGGTZQ1nI6BTYGQGsU/73TkEAm
         |7SvbJZcB2duLSCn8H5D0S1cafory1gnL1TpMPBlY8J/lq/Mht2E17eYw+P87F
         |cpvDShINzy8GxWHqfquBqO8ml4XtirVEtAlI0xlkAsKkVq4nj7rKZUMS85mzo
         |gjUAJn3WgpGCNXVU+EK+qElW5QXk3I9uozByZhwBcYt5Cnlg15o99+53wKzMM
         |mdvFmVjA1DeUaSO7LMIuw4ZNLVdDcHJx7ZSpAKZ/EA34u1fYNECFcw5CSKOjd
         |lU7JFr4o8Phw==""".stripMargin.replaceAll("\n", "")
    val EXPECTED_AUTHENTICATION_HEADER_V2: String =
      s"""MWSV2 $CLIENT_REQUEST_BINARY_APP_UUID:GpZIRB8RIxlfsjcROBElMEw
         |a0r7jr632GkBe+R8lOv72vVV7bFMbJwQUHYm6vL/NKC7g4lJwvWcF60lllIUG
         |wv/KWUOQwerqo5yCNoNumxjgDKjq7ILl8iFxsrV9LdvxwGyEBEwAPKzoTmW9x
         |radxmjn4ZZVMnQKEMns6iViBkwaAW2alp4ZtVfJIZHRRyiuFnITWH1PniyG0k
         |I4Li16kY25VfmzfNkdAi0Cnl27Cy1+DtAl1zVnz6ObMAdtmsEtplvlqsRCRsd
         |d37VfuUxUlolNpr5brjzTwXksScUjX80/HMnui5ZlFORGjHebeZG5QVCouZPK
         |BWTWsELGx1iyaw==;""".stripMargin.replaceAll("\n", "")
    val headers: Map[String, String] =
      mAuthSigner.generateRequestHeaders("PUT", CLIENT_REQUEST_BINARY_PATH, FixturesLoader.getBinaryFileBody, CLIENT_REQUEST_QUERY_PARAMETERS).asScala.toMap
    headers(MAuthRequest.X_MWS_AUTHENTICATION_HEADER_NAME) shouldBe EXPECTED_AUTHENTICATION_HEADER_V1
    headers(MAuthRequest.MCC_AUTHENTICATION_HEADER_NAME) shouldBe EXPECTED_AUTHENTICATION_HEADER_V2
    headers(MAuthRequest.X_MWS_TIME_HEADER_NAME) shouldBe String.valueOf(CLIENT_REQUEST_BINARY_EPOCH_TIME)
    headers(MAuthRequest.MCC_TIME_HEADER_NAME) shouldBe String.valueOf(CLIENT_REQUEST_BINARY_EPOCH_TIME)
  }

}
