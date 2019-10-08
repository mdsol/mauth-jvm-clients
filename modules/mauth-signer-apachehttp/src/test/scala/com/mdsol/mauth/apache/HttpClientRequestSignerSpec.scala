package com.mdsol.mauth.apache

import java.security.Security
import java.util.UUID

import com.mdsol.mauth.MAuthRequest
import com.mdsol.mauth.test.utils.FixturesLoader
import com.mdsol.mauth.util.EpochTimeProvider
import org.apache.http.client.methods.{HttpGet, HttpPost}
import org.apache.http.entity.StringEntity
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.scalamock.scalatest.MockFactory
import org.scalatest.{FlatSpec, Matchers}

class HttpClientRequestSignerSpec extends FlatSpec with Matchers with MockFactory {
  Security.addProvider(new BouncyCastleProvider)

  private val TEST_EPOCH_TIME = 1424700000L
  private val testUUID = UUID.fromString("2a6790ab-f6c6-45be-86fc-9e9be76ec12a")
  private val AUTHENTICATION_HEADER_PATTERN_V2 = "MWSV2 $testUUID:[^;]*;"
  private val TEST_REQUEST_BODY = "Request Body"
  private var privateKeyString = FixturesLoader.getPrivateKey
  private val mockEpochTimeProvider = mock[EpochTimeProvider]
  private val mAuthRequestSigner = new HttpClientRequestSigner(testUUID, privateKeyString, mockEpochTimeProvider)
  private val mAuthRequestSignerV2 = new HttpClientRequestSigner(testUUID, privateKeyString, mockEpochTimeProvider,true)

  behavior of "#signRequest"

  it should "adds expected time header" in {
    //noinspection ConvertibleToMethodValue
    (mockEpochTimeProvider.inSeconds _: () => Long).expects().returns(TEST_EPOCH_TIME)
    val get = new HttpGet("http://mauth.imedidata.com/")
    mAuthRequestSigner.signRequest(get)
    get.getFirstHeader(MAuthRequest.X_MWS_TIME_HEADER_NAME).getValue shouldBe String.valueOf(TEST_EPOCH_TIME)
  }

  it should "adds expected authentication header" in {
    //noinspection ConvertibleToMethodValue
    (mockEpochTimeProvider.inSeconds _: () => Long).expects().returns(TEST_EPOCH_TIME)
    val get = new HttpGet("http://mauth.imedidata.com/")
    mAuthRequestSigner.signRequest(get)
    val EXPECTED_GET_AUTHENTICATION_HEADER =
      s"""MWS $testUUID:bXkxaWM5Src65bVPdv
         |466zC9JIy79aNfjjTczXoT01Tycxkbv/8U/7utTV+HgdJvvA1Du9wDD+l0d
         |hvRb3lmEI1LIp1A4j2rogHc13n9WdV8Q9x381Te7B9uTSdOz1k/9QRZaDrm
         |Fl9GtBq4xe9xQPPF/U0cOFm4R/0OMQCYamf4/mc2PZ6t8ZOCd2gGvR70l1n
         |9PoTTSZaULcul/oR7HFK25FPjsIQ9FkYVjJ+iwKPhrIgcZwUznNL71d+V8b
         |Q2Jr3RK+1c115rlHEy9SgLh1nW8SHP+uzZMApWEFASaLyTePbuvVUDtJbzi
         |WYjVvr4m20PM2aLhMmVYcKU5T288w==""".stripMargin.replaceAll("\n", "")
    get.getFirstHeader(MAuthRequest.X_MWS_AUTHENTICATION_HEADER_NAME).getValue shouldBe EXPECTED_GET_AUTHENTICATION_HEADER
  }

  it should "sign requests with body adds expected authentication header" in {
    //noinspection ConvertibleToMethodValue
    (mockEpochTimeProvider.inSeconds _: () => Long).expects().returns(TEST_EPOCH_TIME)
    val post = new HttpPost("http://mauth.imedidata.com/")
    val EXPECTED_POST_AUTHENTICATION_HEADER =
      s"""MWS $testUUID:aDItoM9IOknNhPKH9a
         |qMguASxjBErA2KzCfiZKjCQx0LyMuNZAQ/6tZWfLZ6tI+XMTV51sxc4qiSp
         |uL6UHK9WomqhPtvSDCJ7KU3Xpoi9iJ4J3VtXu8lxKZYkrUBpV0jttbhRn1H
         |3I7VHwXCdV5ptY3WEL9u1iF3whLqUKGyYxf7WFgJmBbX/V7VIRGOW8BJjfW
         |J9pDVypVBN/VOYWLlKv9o3TTZuuEBtutuBSd6cU4oTMDnQmGkWs9fDAfdkF
         |2l/ZdmD7LFryk9vuyPJ5ur82ksJIZO61fzsEh0Klg/Qcr1E9M0dj0DtxBzw
         |4W0Oc1sXH67xKrKidr3JxuBXjv5gg==""".stripMargin.replaceAll("\n", "")
    post.setEntity(new StringEntity(TEST_REQUEST_BODY))
    mAuthRequestSigner.signRequest(post)
    post.getFirstHeader(MAuthRequest.X_MWS_AUTHENTICATION_HEADER_NAME).getValue shouldBe EXPECTED_POST_AUTHENTICATION_HEADER
  }

  it should "adds expected time header for V2" in {
    //noinspection ConvertibleToMethodValue
    (mockEpochTimeProvider.inSeconds _: () => Long).expects().returns(TEST_EPOCH_TIME)
    val get = new HttpGet("http://mauth.imedidata.com/")
    mAuthRequestSignerV2.signRequest(get)
    get.getFirstHeader(MAuthRequest.MCC_TIME_HEADER_NAME).getValue shouldBe String.valueOf(TEST_EPOCH_TIME)
  }

  it should "sign requests with parameters adds expected authentication header for V2" in {
    //noinspection ConvertibleToMethodValue
    (mockEpochTimeProvider.inSeconds _: () => Long).expects().returns(TEST_EPOCH_TIME)
    val get = new HttpGet("http://mauth.imedidata.com/query?k1=v1&k2=v2")
    mAuthRequestSignerV2.signRequest(get)
    get.getFirstHeader(MAuthRequest.MCC_AUTHENTICATION_HEADER_NAME).getValue matches AUTHENTICATION_HEADER_PATTERN_V2
  }

  it should "sign requests with body adds expected authentication header for V2" in {
    //noinspection ConvertibleToMethodValue
    (mockEpochTimeProvider.inSeconds _: () => Long).expects().returns(TEST_EPOCH_TIME)
    val post = new HttpPost("http://mauth.imedidata.com/")
    post.setEntity(new StringEntity(TEST_REQUEST_BODY))
    mAuthRequestSignerV2.signRequest(post)
    post.getFirstHeader(MAuthRequest.MCC_AUTHENTICATION_HEADER_NAME).getValue matches AUTHENTICATION_HEADER_PATTERN_V2
  }

}
