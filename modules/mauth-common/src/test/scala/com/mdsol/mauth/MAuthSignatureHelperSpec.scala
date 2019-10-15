package com.mdsol.mauth

import java.security.Security
import java.util.UUID

import com.mdsol.mauth.util.MAuthSignatureHelper
import com.mdsol.mauth.util.MAuthKeysHelper.getPrivateKeyFromString
import com.mdsol.mauth.util.MAuthKeysHelper.getPublicKeyFromString
import com.mdsol.mauth.test.utils.FixturesLoader
import org.scalatest.{FlatSpec, Matchers}
import org.bouncycastle.jce.provider.BouncyCastleProvider

class MAuthSignatureHelperSpec extends FlatSpec with Matchers {

  Security.addProvider(new BouncyCastleProvider)

  private val CLIENT_APP_UUID = "92a1869e-c80d-4f06-8775-6c4ebb0758e0"
  private val CLIENT_REQUEST_METHOD = "GET"
  private val CLIENT_REQUEST_PATH = "/resource/path"
  private val CLIENT_REQUEST_PAYLOAD = "message here"
  private val CLIENT_REQUEST_QUERY_PARAMETERS = "key1=value1&key2=value2"
  private val TEST_EPOCH_TIME = 1424700000L
  private val TEST_PRIVATE_KEY = getPrivateKeyFromString(FixturesLoader.getPrivateKey2)

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

    MAuthSignatureHelper.generateStringToSign(
      UUID.fromString(CLIENT_APP_UUID),
      CLIENT_REQUEST_METHOD,
      CLIENT_REQUEST_PATH,
      CLIENT_REQUEST_QUERY_PARAMETERS,
      CLIENT_REQUEST_PAYLOAD,
      String.valueOf(TEST_EPOCH_TIME),
      MAuthVersion.MWSV2.getValue
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
    val queryString = "key=-_.~ !@#$%^*()+{}|:\"'`<>?"
    val expectedString = "key=-_.~%20%21%40%23%24%25%5E%2A%28%29%2B%7B%7D%7C%3A%22%27%60%3C%3E%3F"
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
    val signString = MAuthSignatureHelper.encryptSignatureRSA(getPrivateKeyFromString(FixturesLoader.getPrivateKey), testString)
    MAuthSignatureHelper.verifyRSA(testString, signString, getPublicKeyFromString(FixturesLoader.getPublicKey)) shouldBe true
  }

}
