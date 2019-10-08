package com.mdsol.mauth

import java.util.UUID

import com.mdsol.mauth.util.MAuthSignatureHelper
import org.scalatest.{FlatSpec, Matchers}

class MAuthSignatureHelperSpec extends FlatSpec with Matchers {

  private val CLIENT_APP_UUID = "92a1869e-c80d-4f06-8775-6c4ebb0758e0"
  private val CLIENT_REQUEST_METHOD = "GET"
  private val CLIENT_REQUEST_PATH = "/resource/path"
  private val CLIENT_REQUEST_PAYLOAD = "message here"
  private val CLIENT_REQUEST_QUERY_PARAMETERS = "key1=value1&key2=value2";
  private val TEST_EPOCH_TIME = 1424700000L

  behavior of "MAuthSignatureHelper"

  it should "correctly generate string to sign for mAuth V1" in {
    val expectedString = CLIENT_REQUEST_METHOD + "\n" +
      CLIENT_REQUEST_PATH + "\n" +
      CLIENT_REQUEST_PAYLOAD + "\n" +
      CLIENT_APP_UUID + "\n" + String.valueOf(TEST_EPOCH_TIME)

    MAuthSignatureHelper.generateUnencryptedSignature( UUID.fromString(CLIENT_APP_UUID),
      CLIENT_REQUEST_METHOD, CLIENT_REQUEST_PATH, CLIENT_REQUEST_PAYLOAD, String.valueOf(TEST_EPOCH_TIME)) shouldBe expectedString
   }

  it should "correctly generate string to sign for mAuth V2" in {
    val paylodDigest = MAuthSignatureHelper.getHexEncodedDigestedString("message here")
    val expectedString = CLIENT_REQUEST_METHOD + "\n" +
      CLIENT_REQUEST_PATH + "\n" +
      paylodDigest + "\n" +
      CLIENT_APP_UUID + "\n" + String.valueOf(TEST_EPOCH_TIME) + "\n" +
      CLIENT_REQUEST_QUERY_PARAMETERS

    MAuthSignatureHelper.generateStringToSign( UUID.fromString(CLIENT_APP_UUID),
      CLIENT_REQUEST_METHOD, CLIENT_REQUEST_PATH, CLIENT_REQUEST_QUERY_PARAMETERS, CLIENT_REQUEST_PAYLOAD,
      String.valueOf(TEST_EPOCH_TIME), MAuthVersion.MWSV2.getValue) shouldBe expectedString
  }

  it should "correctly sort query string" in {
    val queryString = "key2=data2&key1=data1&key1=a&key1=9"
    val expectedString = "key1=9&key1=a&key1=data1&key2=data2"
    MAuthSignatureHelper.generateEncryptedQueryParams(queryString) shouldBe expectedString
  }

  it should "correctly encode query string" in {
    val queryString = "key=The string ü@foo-bar"
    val expectedString = "key=The+string+%C3%BC%40foo-bar"
    MAuthSignatureHelper.generateEncryptedQueryParams(queryString) shouldBe expectedString
  }

  it should "correctly encode query string with special chars" in {
    val queryString = "key=-_.~ !@#$%^*()+{}|:\\\"'`<>?"
    val expectedString = "key=-_.%7E+%21%40%23%24%25%5E*%28%29%2B%7B%7D%7C%3A%5C%22%27%60%3C%3E%3F"
    MAuthSignatureHelper.generateEncryptedQueryParams(queryString) shouldBe expectedString
  }

}
