package com.mdsol.mauth.util;

import com.mdsol.mauth.exceptions.MAuthSigningException;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Hex;
import org.bouncycastle.crypto.CryptoException;
import org.bouncycastle.crypto.InvalidCipherTextException;
import org.bouncycastle.crypto.encodings.PKCS1Encoding;
import org.bouncycastle.crypto.engines.RSAEngine;
import org.bouncycastle.crypto.util.PrivateKeyFactory;
import org.bouncycastle.crypto.util.PublicKeyFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.UUID;

public class MAuthSignatureHelper {

  private static final Logger logger = LoggerFactory.getLogger(MAuthSignatureHelper.class);

  public static String generateUnencryptedSignature(UUID appUUID, String httpMethod, String resourceUrl, String body, String epochTime) {
    logger.debug("Generating Unencrypted Signature");
    return httpMethod + "\n" + resourceUrl + "\n" + body + "\n" + appUUID.toString() + "\n" + epochTime;
  }

  public static String encryptSignature(PrivateKey privateKey, String unencryptedString) throws IOException, CryptoException {
    String hexEncodedString = getHexEncodedDigestedString(unencryptedString);

    PKCS1Encoding encryptEngine = new PKCS1Encoding(new RSAEngine());
    encryptEngine.init(true, PrivateKeyFactory.createKey(privateKey.getEncoded()));
    byte[] encryptedStringBytes = encryptEngine.processBlock(hexEncodedString.getBytes(), 0,hexEncodedString.getBytes().length);

    return new String(Base64.encodeBase64(encryptedStringBytes), "UTF-8");
  }

  public static byte[] decryptSignature(PublicKey publicKey, String encryptedSignature) {
    try {
      // Decode the signature from its base 64 form
      byte[] decodedSignature = Base64.decodeBase64(encryptedSignature);

      // Decrypt the signature with public key from requesting application
      PKCS1Encoding decryptEngine = new PKCS1Encoding(new RSAEngine());
      decryptEngine.init(false, PublicKeyFactory.createKey(publicKey.getEncoded()));
      byte[] decryptedSignature;
      decryptedSignature = decryptEngine.processBlock(decodedSignature, 0, decodedSignature.length);

      return decryptedSignature;
    } catch (InvalidCipherTextException | IOException ex) {
      final String msg = "Couldn't decrypt the signature using given public key.";
      logger.error(msg, ex);
      throw new MAuthSigningException(msg, ex);
    }
  }

  public static String getHexEncodedDigestedString(String unencryptedString) {
    try {
      // Get digest
      MessageDigest md = MessageDigest.getInstance("SHA-512", "BC");
      byte[] digestedString = md.digest(unencryptedString.getBytes(StandardCharsets.UTF_8));
      // Convert to hex
      return Hex.encodeHexString(digestedString);
    } catch (NoSuchAlgorithmException | NoSuchProviderException ex) {
      final String message = "Invalid alghoritm or security provider.";
      logger.error(message, ex);
      throw new MAuthSigningException(message, ex);
    }
  }
}
