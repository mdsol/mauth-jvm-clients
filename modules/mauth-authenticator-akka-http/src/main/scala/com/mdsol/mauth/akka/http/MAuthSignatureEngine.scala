package com.mdsol.mauth.akka.http

import java.io.IOException
import java.nio.charset.StandardCharsets
import java.security.{MessageDigest, PublicKey}
import java.util.UUID

import com.mdsol.mauth.CryptoError
import com.typesafe.scalalogging.StrictLogging
import org.apache.commons.codec.binary.{Base64, Hex}
import org.bouncycastle.crypto.InvalidCipherTextException
import org.bouncycastle.crypto.encodings.PKCS1Encoding
import org.bouncycastle.crypto.engines.RSAEngine
import org.bouncycastle.crypto.util.PublicKeyFactory

/**
  * Signature engine for MAuth Specification. Currently only String
  * bodies (entities) are supported in MAuth
  */
private[mauth] object MAuthSignatureEngine extends StrictLogging {

  /**
    * Get the timestamp in MAuth standard format
    *
    * @return Whole seconds since the epoch
    */
  def getEpochTime: String = (System.currentTimeMillis() / 1000).toString

  /**
    * Create a signature based on the supplied request parameters
    *
    * @param appUUID     The UUID of the application (client) using the API
    * @param httpMethod  The HTTP verb of the request
    * @param resourceUrl The URI path of the request
    * @param body        The Payload of the entity as a String
    * @param epochTime   The current epoch time (whole seconds) in String form
    * @return The signature of this request
    */
  def buildSignature(appUUID: UUID, httpMethod: String, resourceUrl: String, body: String, epochTime: String): String = {
    val signature = httpMethod + "\n" + resourceUrl + "\n" + body + "\n" + appUUID.toString + "\n" + epochTime
    signature
  }

  /**
    * Decrypt a message digest using the public key of the third party
    *
    * @param encDigestBase64 Base 64 encoded encrypted digest
    * @param publicKey       The public key used to decrypt the digest
    * @return
    */
  def decryptFromBase64(encDigestBase64: String, publicKey: PublicKey): Either[CryptoError, Array[Byte]] = {
    try {
      val encryptedDigest: Array[Byte] = Base64.decodeBase64(encDigestBase64)
      val decryptEngine: PKCS1Encoding = new PKCS1Encoding(new RSAEngine())
      decryptEngine.init(false, PublicKeyFactory.createKey(publicKey.getEncoded))
      val decryptedDigest: Array[Byte] = decryptEngine.processBlock(encryptedDigest, 0, encryptedDigest.length)
      Right(decryptedDigest)
    } catch {
      case e: IOException =>
        logger.warn(s"IOException decrypting the signature : ${e.getMessage}", e)
        Left(CryptoError(s"IOException decrypting the signature : ${e.getMessage}", Some(e)))
      case e: InvalidCipherTextException =>
        logger.warn(s"InvalidCipherTextException decrypting the signature : ${e.getMessage}", e)
        Left(CryptoError(s"InvalidCipherTextException decrypting the signature : ${e.getMessage}", Some(e)))
    }
  }

  /**
    * Convenience method for server side digest authentication
    *
    * @param base64Header    Base 64 value taken directly from the authentication header (minus prefix and UUID)
    * @param signatureString The signature String from the buildSignature method
    * @return Boolean true if there is a match, false if not
    */
  def compareDigests(base64Header: String, key: PublicKey, signatureString: String): Boolean = {
    decryptFromBase64(base64Header, key) match {
      case Left(CryptoError(msg, Some(e))) => logger.debug(msg + " : " + e.getMessage, e); false
      case Left(CryptoError(msg, None)) => logger.debug(msg); false
      case Right(headerDigest: Array[Byte]) =>
        val newDigest = asHex(getDigest(signatureString))
        java.util.Arrays.equals(newDigest.getBytes("UTF-8"), headerDigest)
    }
  }


  /**
    * Convert a plaintext request signature into a hex encoded digest String
    *
    * @param signature
    * @return Hex encoded digest string
    */
  def getDigest(signature: String): Array[Byte] = {
    val messageDigest = MessageDigest.getInstance("SHA-512", "BC")
    messageDigest.digest(signature.getBytes(StandardCharsets.UTF_8))
  }

  /**
    * Convenience method
    *
    * @param array
    * @return
    */
  def asHex(array: Array[Byte]): String = {
    Hex.encodeHexString(array)
  }

  /**
    * Convenience method
    *
    * @param array
    * @return
    */
  def fromHex(array: Array[Char]): Array[Byte] = {
    Hex.decodeHex(array)
  }
}
