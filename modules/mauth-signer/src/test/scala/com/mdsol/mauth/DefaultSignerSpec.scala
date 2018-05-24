package com.mdsol.mauth

import java.security.Security
import java.util.UUID

import com.mdsol.mauth.exceptions.MAuthKeyException
import com.mdsol.mauth.test.utils.FixturesLoader
import com.mdsol.mauth.util.EpochTimeProvider
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.scalamock.scalatest.MockFactory
import org.scalatest.{FlatSpec, Matchers}

import scala.collection.JavaConverters._

class DefaultSignerSpec extends FlatSpec with Matchers with MockFactory {
  private val TEST_EPOCH_TIME = 1424700000L
  private val testUUID = UUID.fromString("2a6790ab-f6c6-45be-86fc-9e9be76ec12a")
  private val mockEpochTimeProvider = mock[EpochTimeProvider]
  private val mAuthRequestSigner = new DefaultSigner(testUUID, FixturesLoader.getPrivateKey, mockEpochTimeProvider)

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
        |DtJbziWYjVvr4m20PM2aLhMmVYcKU5T288w==""".stripMargin.replaceAll("\n","")
    headers(MAuthRequest.X_MWS_AUTHENTICATION_HEADER_NAME) shouldBe EXPECTED_GET_AUTHENTICATION_HEADER
  }

  it should "generated headers with body includes expected authentication header" in {
    //noinspection ConvertibleToMethodValue
    (mockEpochTimeProvider.inSeconds _: () => Long).expects().returns(TEST_EPOCH_TIME)

    val TEST_REQUEST_BODY: String = "Request Body"
    val headers: Map[String, String] = mAuthRequestSigner.generateRequestHeaders("POST", "/", TEST_REQUEST_BODY).asScala.toMap
    val EXPECTED_POST_AUTHENTICATION_HEADER: String =
      s"""MWS $testUUID:aDItoM9IOknNhPKH9a
        |qMguASxjBErA2KzCfiZKjCQx0LyMuNZAQ/6tZWfLZ6tI+XMTV51sxc4qiSp
        |uL6UHK9WomqhPtvSDCJ7KU3Xpoi9iJ4J3VtXu8lxKZYkrUBpV0jttbhRn1H
        |3I7VHwXCdV5ptY3WEL9u1iF3whLqUKGyYxf7WFgJmBbX/V7VIRGOW8BJjfW
        |J9pDVypVBN/VOYWLlKv9o3TTZuuEBtutuBSd6cU4oTMDnQmGkWs9fDAfdkF
        |2l/ZdmD7LFryk9vuyPJ5ur82ksJIZO61fzsEh0Klg/Qcr1E9M0dj0DtxBzw
        |4W0Oc1sXH67xKrKidr3JxuBXjv5gg==""".stripMargin.replaceAll("\n","")
    headers(MAuthRequest.X_MWS_AUTHENTICATION_HEADER_NAME) shouldBe EXPECTED_POST_AUTHENTICATION_HEADER
  }
}
