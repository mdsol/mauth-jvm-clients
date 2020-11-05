package com.mdsol.mauth

import java.nio.charset.StandardCharsets
import java.security.Security
import java.util.UUID

import com.mdsol.mauth.test.utils.TestFixtures
import com.mdsol.mauth.util.MAuthKeysHelper.{getPrivateKeyFromString, getPublicKeyFromString}
import com.mdsol.mauth.util.MAuthSignatureHelper
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class MAuthSignatureHelperSpec extends AnyFlatSpec with Matchers {

  Security.addProvider(new BouncyCastleProvider)

  private val CLIENT_APP_UUID = "92a1869e-c80d-4f06-8775-6c4ebb0758e0"
  private val CLIENT_REQUEST_METHOD = "GET"
  private val CLIENT_REQUEST_PATH = "/resource/path"
  private val CLIENT_REQUEST_PAYLOAD = "message here"
  private val CLIENT_REQUEST_QUERY_PARAMETERS = "key1=value1&key2=value2"
  private val TEST_EPOCH_TIME = 1424700000L
  private val TEST_PRIVATE_KEY = getPrivateKeyFromString(TestFixtures.PRIVATE_KEY_2)

  // the same test data with ruby and python
  private val CLIENT_APP_UUID_V2 = TestFixtures.APP_UUID_V2
  private val CLIENT_REQUEST_METHOD_V2 = TestFixtures.REQUEST_METHOD_V2
  private val CLIENT_REQUEST_PATH_V2 = TestFixtures.REQUEST_PATH_V2
  private val CLIENT_REQUEST_QUERY_PARAMETERS_V2 = TestFixtures.REQUEST_QUERY_PARAMETERS_V2
  private val TEST_EPOCH_TIME_V2 = TestFixtures.EPOCH_TIME

  behavior of "MAuthSignatureHelper"

  it should "correctly generate string to sign for mAuth V1" in {
    val expectedString = CLIENT_REQUEST_METHOD + "\n" +
      CLIENT_REQUEST_PATH + "\n" +
      CLIENT_REQUEST_PAYLOAD + "\n" +
      CLIENT_APP_UUID + "\n" + String.valueOf(TEST_EPOCH_TIME)

    MAuthSignatureHelper.generateUnencryptedSignature(
      UUID.fromString(CLIENT_APP_UUID),
      CLIENT_REQUEST_METHOD,
      CLIENT_REQUEST_PATH,
      CLIENT_REQUEST_PAYLOAD,
      String.valueOf(TEST_EPOCH_TIME)
    ) shouldBe expectedString
  }

  it should "correctly generate string to sign for mAuth V2" in {
    val paylodDigest = MAuthSignatureHelper.getHexEncodedDigestedString("message here")
    val expectedString = CLIENT_REQUEST_METHOD + "\n" +
      CLIENT_REQUEST_PATH + "\n" +
      paylodDigest + "\n" +
      CLIENT_APP_UUID + "\n" + String.valueOf(TEST_EPOCH_TIME) + "\n" +
      CLIENT_REQUEST_QUERY_PARAMETERS

    MAuthSignatureHelper.generateStringToSignV2(
      UUID.fromString(CLIENT_APP_UUID),
      CLIENT_REQUEST_METHOD,
      CLIENT_REQUEST_PATH,
      CLIENT_REQUEST_QUERY_PARAMETERS,
      CLIENT_REQUEST_PAYLOAD.getBytes(StandardCharsets.UTF_8),
      String.valueOf(TEST_EPOCH_TIME)
    ) shouldBe expectedString
  }

  it should "correctly encode query string sort by code point" in {
    val queryString = "∞=v&キ=v&0=v&a=v"
    val expectedString = "0=v&a=v&%E2%88%9E=v&%E3%82%AD=v"
    MAuthSignatureHelper.generateEncryptedQueryParams(queryString) shouldBe expectedString
  }

  it should "correctly encode query string sort by value if keys are the same" in {
    val queryString = "a=b&a=c&a=a"
    val expectedString = "a=a&a=b&a=c"
    MAuthSignatureHelper.generateEncryptedQueryParams(queryString) shouldBe expectedString
  }

  it should "correctly encode query string with empty values" in {
    val queryString = "k=&k=v"
    MAuthSignatureHelper.generateEncryptedQueryParams(queryString) shouldBe queryString
  }

  it should "correctly encode query string" in {
    val queryString = "key=The string ü@foo-bar"
    val expectedString = "key=The%20string%20%C3%BC%40foo-bar"
    MAuthSignatureHelper.generateEncryptedQueryParams(queryString) shouldBe expectedString
  }

  it should "correctly encode query string with special chars" in {
    val queryString = "key2=asdf+f&key=%21%40%23"
    val expectedString = "key=%21%40%23&key2=asdf%20f"
    MAuthSignatureHelper.generateEncryptedQueryParams(queryString) shouldBe expectedString
  }

  it should "correctly generate signature for V1" in {
    val testString = "Hello world"
    val expectedString = "F/GAuGYEykrtrmIE/XtETSi0QUoKxUwwTXljT1tUiqNHmyH2NRhKQ1flqusaB7H" +
      "6bwPBb+FzXzfmiO32lJs6SxMjltqM/FjwucVNhn1BW+KXFnZniPh3M0+FwwspksX9xc/KcWEPebtIIEM5c" +
      "X2rBl43xlvwYtS/+D+obo1AVPv2l5qd+Gwl9b61kYF/aoPGx+bVnmWZK8e8BZxZOjjGjmQAOYRYgGWzolL" +
      "LnzIZ6xy6efY3D9jPXXDqgnqWQvwLStkKJIydrkXUTd0m36X6mD00qHgI7xoYSLgqxNSg1EgO8yuette8B" +
      "Kl9D+YbIEJ3xFnaZmCfVGks0M9tmZ2PXg==".stripMargin.replaceAll("\n", "")
    MAuthSignatureHelper.encryptSignature(TEST_PRIVATE_KEY, testString) shouldBe expectedString
  }

  it should "correctly generate signature for V1 Unicode" in {
    val testString = "こんにちはÆ"
    val expectedString = ("cHrT3G7zCA2aRcY5jtvNlm0orafBOn924rQ9aSQS1lvNCwbg/LMnTsV+jHZUtOy" +
      "DFSvErBwd9ga1FrsjOQDfhNoU1K+pVQ11nHU23cHQi0bsYByKPIDh1jMW4wNtP+A7Z/Xh0CIESBc+SaeIjP" +
      "znMunocwci34kN4AXWudkZ2+xZxqfZiX6TVrwmREppsgoZD2ODVt6FtnBvcGd0sRAa9A3Iy+EaB8wOM5kaU" +
      "yusfGcxeCYuCGN1FHjd1AkBkm2I4wbsxQInKDyYQXjMv3fA5oMw4nxhL/AJzUx3xWWCG5pub1/YB3jWwQgt" +
      "Gjpxvb5LhHT9S/JtuT8RU01JukC8dQ==").stripMargin.replaceAll("\n", "")
    MAuthSignatureHelper.encryptSignature(TEST_PRIVATE_KEY, testString) shouldBe expectedString
  }

  it should "correctly generate signature for V2" in {
    val testString = "Hello world"
    val expectedString = ("KODkSEnqjr52EWOFvrRj2igwMR8EHsFYpBzDSEWge7UenB3u8OKP1nXeg1oJ0X" +
      "1z8S+fpODMOh6NaGalEZgoyk0VRZ/BhFRiOg/xCMm6DA2J48EtBt8DYONVKTp4W2e2OU68NMGlj2upkjSs" +
      "iD8MoIu2SHYwdkjx4PwKl2sPbQtKnsyl6kgSfhGd+1WsgTELDfeNdy3mSX7iJtKkpmUV5DZ1P0BcPCLbh/" +
      "2KfAHx4sDIHFUf+U06ei/WVNzz1l5+fpwE0EV/lxtMLcCFUVQlM9li8Yjpsh0EbwzuV24pMB0xhwvci4B7" +
      "JSYbLK76JUBthhwzUtXzyuzfQi4lNeXR7g==").stripMargin.replaceAll("\n", "")
    MAuthSignatureHelper.encryptSignatureRSA(TEST_PRIVATE_KEY, testString) shouldBe expectedString
  }

  it should "correctly generate signature for V2 Unicode" in {
    val testString = "こんにちはÆ"
    val expectedString = ("F9OqgCXr6vKAVBoU8Iogg09HhMZ+FpcJ8Q8DJ/M82vCDjVdxYQ1BYpuyXWN2jI" +
      "H5CWKnYvXxF49aKwiXuo7bgUArNZZJuwRzI5hSEwsY6weVzlsO8DmdDR62MKozK9NBEr7nnVka8NFEWrpr" +
      "WNPrgvy//YK5NAPSt+tLq/7qk5+qJZRjAjAhl09FD2pzYNGZkLx24UuPPfPSkvQKcybcAgY5y17FNkQTYY" +
      "udjBy2hG6Df+Op77VjKx5yaLHZfoKcOmxc6UdE09kkoS5rsW2Y65kLi4xWbLK3i+VUC+WCqL8Vt7McJFMA" +
      "wOyACDJPr4Z3VtHUZgnT9b5n7c7U/CItRg==").stripMargin.replaceAll("\n", "")
    MAuthSignatureHelper.encryptSignatureRSA(TEST_PRIVATE_KEY, testString) shouldBe expectedString
  }

  it should "verify signature for V2" in {
    val testString = "Hello world"
    val signString = MAuthSignatureHelper.encryptSignatureRSA(getPrivateKeyFromString(TestFixtures.PRIVATE_KEY_1), testString)
    MAuthSignatureHelper.verifyRSA(testString, signString, getPublicKeyFromString(TestFixtures.PUBLIC_KEY_1)) shouldBe true
  }

  it should "correctly generate signature of binary body for V2" in {
    val testString = MAuthSignatureHelper.generateStringToSignV2(
      UUID.fromString(CLIENT_APP_UUID_V2),
      CLIENT_REQUEST_METHOD_V2,
      CLIENT_REQUEST_PATH_V2,
      CLIENT_REQUEST_QUERY_PARAMETERS_V2,
      TestFixtures.BINARY_FILE_BODY,
      TEST_EPOCH_TIME_V2
    )
    MAuthSignatureHelper.encryptSignatureRSA(TEST_PRIVATE_KEY, testString) shouldBe TestFixtures.SIGNATURE_V2_BINARY
  }

  it should "correctly generate signature with empty body for V2" in {
    val testString = MAuthSignatureHelper.generateStringToSignV2(
      UUID.fromString(CLIENT_APP_UUID_V2),
      CLIENT_REQUEST_METHOD_V2,
      CLIENT_REQUEST_PATH_V2,
      CLIENT_REQUEST_QUERY_PARAMETERS_V2,
      Array.empty,
      TEST_EPOCH_TIME_V2
    )
    MAuthSignatureHelper.encryptSignatureRSA(TEST_PRIVATE_KEY, testString) shouldBe TestFixtures.SIGNATURE_V2_EMPTY
  }

  it should "normalize path: correctly generate signature for V2" in {
    val pathList = Array("/./v1/pictures", "/v1//pictures")
    for (resourcePath <- pathList) {
      val testString = MAuthSignatureHelper.generateStringToSignV2(
        UUID.fromString(CLIENT_APP_UUID_V2),
        CLIENT_REQUEST_METHOD_V2,
        resourcePath,
        CLIENT_REQUEST_QUERY_PARAMETERS_V2,
        TestFixtures.BINARY_FILE_BODY,
        TEST_EPOCH_TIME_V2
      )
      MAuthSignatureHelper.encryptSignatureRSA(TEST_PRIVATE_KEY, testString) shouldBe TestFixtures.SIGNATURE_V2_BINARY
    }
  }

  it should "correctly normalize paths" in {
    // define of test cases: testString, expectedString
    val testCases = Array(
      Array("/example/sample", "/example/sample"),
      Array("/example/sample/..", "/example/"),
      Array("/example/sample/../..", "/"),
      Array("/example/sample/../../../..", "/"),
      Array("/example//sample/", "/example/sample/"),
      Array("//example///sample/", "/example/sample/"),
      Array("/example//./.", "/example/"),
      Array("/./example/./.", "/example/"),
      Array("/%2a%80", "/%2A%80"),
      Array("/example/", "/example/")
    )
    for (i <- 0 to testCases.length - 1)
      MAuthSignatureHelper.normalizePath(testCases(i)(0)) shouldBe testCases(i)(1)
  }

  it should "correctly encode, sort query parameters" in {
    // define of test cases: testString, expectedString
    val testCases = Array(
      Array("k=%7E", "k=~"),
      Array("k=%20", "k=%20"),
      Array("k=&k=v", "k=&k=v"),
      Array("k=%7E&k=~&k=%40&k=a", "k=%40&k=a&k=~&k=~"),
      Array("a=b&a=c&a=a", "a=a&a=b&a=c"),
      Array("", "")
    )
    for (i <- 0 to testCases.length - 1)
      MAuthSignatureHelper.generateEncryptedQueryParams(testCases(i)(0)) shouldBe testCases(i)(1)
  }
}
