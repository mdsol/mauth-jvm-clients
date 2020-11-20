package com.mdsol.mauth.apache

import java.security.Security
import java.util.UUID

import com.mdsol.mauth.{MAuthRequest, MAuthVersion, SignerConfiguration}
import com.mdsol.mauth.test.utils.TestFixtures
import com.mdsol.mauth.util.EpochTimeProvider
import org.apache.http.client.methods.{HttpGet, HttpPost}
import org.apache.http.entity.{InputStreamEntity, StringEntity}
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.scalamock.scalatest.MockFactory
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import java.io.ByteArrayInputStream
import java.nio.charset.StandardCharsets

class HttpClientRequestSignerSpec extends AnyFlatSpec with Matchers with MockFactory {
  Security.addProvider(new BouncyCastleProvider)

  private val TEST_EPOCH_TIME = 1424700000L
  private val testUUID = UUID.fromString("2a6790ab-f6c6-45be-86fc-9e9be76ec12a")
  private val AUTHENTICATION_HEADER_PATTERN_V2 = s"MWSV2 $testUUID:[^;]*;"
  private val TEST_REQUEST_BODY = "Request Body"
  private val privateKeyString = TestFixtures.PRIVATE_KEY_1
  private val mockEpochTimeProvider = mock[EpochTimeProvider]
  private val mAuthRequestSigner = new HttpClientRequestSigner(testUUID, privateKeyString, mockEpochTimeProvider, SignerConfiguration.ALL_SIGN_VERSIONS)
  private val mAuthRequestSignerV2 = new HttpClientRequestSigner(testUUID, privateKeyString, mockEpochTimeProvider, java.util.Arrays.asList(MAuthVersion.MWSV2))

  private val EXPECTED_GET_AUTHENTICATION_HEADER =
    s"""MWS $testUUID:bXkxaWM5Src65bVPdv
       |466zC9JIy79aNfjjTczXoT01Tycxkbv/8U/7utTV+HgdJvvA1Du9wDD+l0d
       |hvRb3lmEI1LIp1A4j2rogHc13n9WdV8Q9x381Te7B9uTSdOz1k/9QRZaDrm
       |Fl9GtBq4xe9xQPPF/U0cOFm4R/0OMQCYamf4/mc2PZ6t8ZOCd2gGvR70l1n
       |9PoTTSZaULcul/oR7HFK25FPjsIQ9FkYVjJ+iwKPhrIgcZwUznNL71d+V8b
       |Q2Jr3RK+1c115rlHEy9SgLh1nW8SHP+uzZMApWEFASaLyTePbuvVUDtJbzi
       |WYjVvr4m20PM2aLhMmVYcKU5T288w==""".stripMargin.replaceAll("\n", "")

  private val EXPECTED_POST_AUTHENTICATION_HEADER =
    s"""MWS $testUUID:aDItoM9IOknNhPKH9a
       |qMguASxjBErA2KzCfiZKjCQx0LyMuNZAQ/6tZWfLZ6tI+XMTV51sxc4qiSp
       |uL6UHK9WomqhPtvSDCJ7KU3Xpoi9iJ4J3VtXu8lxKZYkrUBpV0jttbhRn1H
       |3I7VHwXCdV5ptY3WEL9u1iF3whLqUKGyYxf7WFgJmBbX/V7VIRGOW8BJjfW
       |J9pDVypVBN/VOYWLlKv9o3TTZuuEBtutuBSd6cU4oTMDnQmGkWs9fDAfdkF
       |2l/ZdmD7LFryk9vuyPJ5ur82ksJIZO61fzsEh0Klg/Qcr1E9M0dj0DtxBzw
       |4W0Oc1sXH67xKrKidr3JxuBXjv5gg==""".stripMargin.replaceAll("\n", "")

  behavior of "#signRequest"

  "HttpClientRequestSigner" should "adds expected time header" in {
    //noinspection ConvertibleToMethodValue
    (mockEpochTimeProvider.inSeconds _: () => Long).expects().returns(TEST_EPOCH_TIME)
    val get = new HttpGet("http://mauth.imedidata.com/")
    mAuthRequestSigner.signRequest(get)
    get.getFirstHeader(MAuthRequest.X_MWS_TIME_HEADER_NAME).getValue shouldBe String.valueOf(TEST_EPOCH_TIME)
    get.getFirstHeader(MAuthRequest.MCC_TIME_HEADER_NAME).getValue shouldBe String.valueOf(TEST_EPOCH_TIME)
  }

  it should "adds expected authentication header" in {
    //noinspection ConvertibleToMethodValue
    (mockEpochTimeProvider.inSeconds _: () => Long).expects().returns(TEST_EPOCH_TIME)
    val get = new HttpGet("http://mauth.imedidata.com/")
    mAuthRequestSigner.signRequest(get)
    get.getFirstHeader(MAuthRequest.X_MWS_AUTHENTICATION_HEADER_NAME).getValue shouldBe EXPECTED_GET_AUTHENTICATION_HEADER
  }

  it should "sign requests with body adds expected authentication header" in {
    //noinspection ConvertibleToMethodValue
    (mockEpochTimeProvider.inSeconds _: () => Long).expects().returns(TEST_EPOCH_TIME)
    val post = new HttpPost("http://mauth.imedidata.com/")
    post.setEntity(new StringEntity(TEST_REQUEST_BODY))
    mAuthRequestSigner.signRequest(post)
    post.getFirstHeader(MAuthRequest.X_MWS_AUTHENTICATION_HEADER_NAME).getValue shouldBe EXPECTED_POST_AUTHENTICATION_HEADER
  }

  "When V1 and V2 are set" should "sign requests adds expected headers for V1 and V2" in {
    //noinspection ConvertibleToMethodValue
    (mockEpochTimeProvider.inSeconds _: () => Long).expects().returns(TEST_EPOCH_TIME)
    val mAuthSigner = new HttpClientRequestSigner(testUUID, privateKeyString, mockEpochTimeProvider, SignerConfiguration.ALL_SIGN_VERSIONS)

    val get = new HttpGet("http://mauth.imedidata.com/query?k1=v1&k2=v2")
    mAuthSigner.signRequest(get)
    get.getFirstHeader(MAuthRequest.X_MWS_AUTHENTICATION_HEADER_NAME).getValue should not be empty
    get.getFirstHeader(MAuthRequest.MCC_AUTHENTICATION_HEADER_NAME).getValue should not be empty
    get.getFirstHeader(MAuthRequest.X_MWS_TIME_HEADER_NAME).getValue shouldBe String.valueOf(TEST_EPOCH_TIME)
    get.getFirstHeader(MAuthRequest.MCC_TIME_HEADER_NAME).getValue shouldBe String.valueOf(TEST_EPOCH_TIME)
  }

  "When V2 only is set" should "adds expected time header for V2 only" in {
    //noinspection ConvertibleToMethodValue
    (mockEpochTimeProvider.inSeconds _: () => Long).expects().returns(TEST_EPOCH_TIME)
    val get = new HttpGet("http://mauth.imedidata.com/")
    mAuthRequestSignerV2.signRequest(get)
    get.getFirstHeader(MAuthRequest.MCC_TIME_HEADER_NAME).getValue shouldBe String.valueOf(TEST_EPOCH_TIME)
    get.getHeaders(MAuthRequest.X_MWS_TIME_HEADER_NAME).isEmpty shouldBe true
  }

  it should "sign requests with parameters adds expected authentication header for V2 only" in {
    //noinspection ConvertibleToMethodValue
    (mockEpochTimeProvider.inSeconds _: () => Long).expects().returns(TEST_EPOCH_TIME)
    val get = new HttpGet("http://mauth.imedidata.com/query?k1=v1&k2=v2")
    mAuthRequestSignerV2.signRequest(get)
    get.getFirstHeader(MAuthRequest.MCC_AUTHENTICATION_HEADER_NAME).getValue matches AUTHENTICATION_HEADER_PATTERN_V2
    get.getHeaders(MAuthRequest.X_MWS_AUTHENTICATION_HEADER_NAME).isEmpty shouldBe true
  }

  it should "sign requests with body adds expected authentication header for V2" in {
    //noinspection ConvertibleToMethodValue
    (mockEpochTimeProvider.inSeconds _: () => Long).expects().returns(TEST_EPOCH_TIME)
    val post = new HttpPost("http://mauth.imedidata.com/")
    post.setEntity(new StringEntity(TEST_REQUEST_BODY))
    mAuthRequestSignerV2.signRequest(post)
    post.getFirstHeader(MAuthRequest.MCC_AUTHENTICATION_HEADER_NAME).getValue matches AUTHENTICATION_HEADER_PATTERN_V2
  }

  it should "sign requests with stream-body adds expected authentication header for V2" in {
    //noinspection ConvertibleToMethodValue
    (mockEpochTimeProvider.inSeconds _: () => Long).expects().returns(TEST_EPOCH_TIME)
    val post = new HttpPost("http://mauth.imedidata.com/")
    val stream = new ByteArrayInputStream(TEST_REQUEST_BODY.getBytes(StandardCharsets.UTF_8))
    post.setEntity(new InputStreamEntity(stream))
    mAuthRequestSignerV2.signRequest(post)
    post.getFirstHeader(MAuthRequest.MCC_AUTHENTICATION_HEADER_NAME).getValue matches AUTHENTICATION_HEADER_PATTERN_V2
  }

  it should "sign requests adds expected headers for V2 with the encoded-normalize path" in {
    //noinspection ConvertibleToMethodValue
    val TEST_UUID = TestFixtures.APP_UUID_V2
    val request_time = TestFixtures.EPOCH_TIME.toLong
    val EXPECTED_SIGNATURE_V2 = TestFixtures.SIGNATURE_NORMALIZE_PATH_V2
    val EXPECTED_AUTHENTICATION_HEADER = s"""MWSV2 $TEST_UUID:$EXPECTED_SIGNATURE_V2;"""

    (mockEpochTimeProvider.inSeconds _: () => Long).expects().returns(request_time)
    val mAuthSigner =
      new HttpClientRequestSigner(UUID.fromString(TEST_UUID), TestFixtures.PRIVATE_KEY_2, mockEpochTimeProvider, java.util.Arrays.asList(MAuthVersion.MWSV2))

    val get = new HttpGet("http://mauth.imedidata.com" + TestFixtures.REQUEST_NORMALIZE_PATH)
    mAuthSigner.signRequest(get)
    get.getFirstHeader(MAuthRequest.MCC_AUTHENTICATION_HEADER_NAME).getValue shouldBe EXPECTED_AUTHENTICATION_HEADER
    get.getFirstHeader(MAuthRequest.MCC_TIME_HEADER_NAME).getValue shouldBe String.valueOf(request_time)
  }

  "When v1 only is set" should "add mauth headers to a request for V1 only" in {
    (mockEpochTimeProvider.inSeconds _: () => Long).expects().returns(TEST_EPOCH_TIME)
    val get = new HttpGet("http://mauth.imedidata.com/")
    val mAuthSingerV1 = new HttpClientRequestSigner(testUUID, privateKeyString, mockEpochTimeProvider, java.util.Arrays.asList(MAuthVersion.MWS))
    mAuthSingerV1.signRequest(get)
    get.getFirstHeader(MAuthRequest.X_MWS_AUTHENTICATION_HEADER_NAME).getValue shouldBe EXPECTED_GET_AUTHENTICATION_HEADER
    get.getFirstHeader(MAuthRequest.X_MWS_TIME_HEADER_NAME).getValue shouldBe String.valueOf(TEST_EPOCH_TIME)
    get.getHeaders(MAuthRequest.MCC_AUTHENTICATION_HEADER_NAME).isEmpty shouldBe true
    get.getHeaders(MAuthRequest.MCC_TIME_HEADER_NAME).isEmpty shouldBe true
  }

  it should "sign requests with stream-body adds expected authentication header" in {
    //noinspection ConvertibleToMethodValue
    (mockEpochTimeProvider.inSeconds _: () => Long).expects().returns(TEST_EPOCH_TIME)
    val post = new HttpPost("http://mauth.imedidata.com/")
    val stream = new ByteArrayInputStream(TEST_REQUEST_BODY.getBytes(StandardCharsets.UTF_8))
    post.setEntity(new InputStreamEntity(stream))
    val mAuthSingerV1 = new HttpClientRequestSigner(testUUID, privateKeyString, mockEpochTimeProvider, java.util.Arrays.asList(MAuthVersion.MWS))
    mAuthSingerV1.signRequest(post)
    post.getFirstHeader(MAuthRequest.X_MWS_AUTHENTICATION_HEADER_NAME).getValue shouldBe EXPECTED_POST_AUTHENTICATION_HEADER
  }

}
