package com.mdsol.mauth

import java.net.URI
import java.security.Security
import java.util.UUID

import com.mdsol.mauth.test.utils.FixturesLoader
import com.mdsol.mauth.util.EpochTimeProvider
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.scalatest.{FlatSpec, Matchers}

class MAuthRequestSignerTest extends FlatSpec with Matchers {
  val TIME_CONSTANT = 1509041057L

  Security.addProvider(new BouncyCastleProvider)

  val signer = MAuthRequestSigner(UUID.fromString("2a6790ab-f6c6-45be-86fc-9e9be76ec12a"), FixturesLoader.getPrivateKey,
    new EpochTimeProvider() {
    override def inSeconds(): Long = TIME_CONSTANT
  })

  "MAuthRequestSigner" should "add time header to a request" in {
    signer.signRequest(UnsignedRequest(uri = new URI("/"))).right.get.timeHeader shouldBe "1509041057"
  }

  it should "add authentication header to a request" in {
    signer.signRequest(UnsignedRequest(uri = new URI("/"))).right.get.authHeader shouldBe "MWS " +
      "2a6790ab-f6c6-45be-86fc-9e9be76ec12a:ih3xq6OvQ2/D5ktPDaZ4F6tanzdn2XGzZ+KOaFXC+YKVjNcSCfUiKB" +
      "6T/552K3AmKm/yZF4rdEOpsMZ0QkuFqEZdwQ8R3iWUwdrNPsmNXSVvF50pRAlcI77UP5gUKV01xjZxfZ/M/vhzVn513" +
      "bAgJ6CM8X4dtG20ki5xLsO35e2eZs5i9IwA/hEaKSm/PH2pEHwxz5c9MMGtHiFgzgXGacziVn0fr2c6X5jb3cDjHnfN" +
      "VX8o57kFjL5E0YOoeEKDwHyflGhbfFNZys29jo83JCK2MQj9s+fZq5NsgmwuACRE6BnqKSPqwDWN4OK3N/iPcTwCsMKz/c5/3CEbMTlH8A=="
  }

  it should "add authentication header to a request with body" in {
    signer.signRequest(UnsignedRequest(uri = new URI("/"), body = Some("Request Body"))).right.get.authHeader shouldBe "MWS " +
      "2a6790ab-f6c6-45be-86fc-9e9be76ec12a:OoxiQ/Z6EjTUAoAGNKD5FS6ka+9IcWW5rtuzbXRDLRGj4pzSdeI0FPIlT0E/ZR96xR0a5EJlJ3E8usr" +
      "5qas/uzNEDajAqpjqOaO4m3j+4juXt0QrdBvj3sgStD6ozOJrfhyeSWvFp3d9SBx8tPkPrqv6z5ewQliaSOaI20yir4+RStwj6P7j/5/ZlDRMBEFBiFuA" +
      "yAWMAbKnefRwK+0yUqO9pEBQx43YqBzs+Xb9sTM0hKd5IohAW8O8xj1coBYP/NGRvhM5Z+VMXnbRXwkqUlEXIDvZ3fKjPNGEQxo+m9oFH1dLI8oGI9xoC9P3liwUqY5h+g+hSQ4KLIfDm0qvLQ=="
  }
}
