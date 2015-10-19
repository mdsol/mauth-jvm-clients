package com.mdsol.mauth.utils;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Hex;
import org.bouncycastle.crypto.CryptoException;
import org.bouncycastle.crypto.encodings.PKCS1Encoding;
import org.bouncycastle.crypto.engines.RSAEngine;
import org.bouncycastle.crypto.util.PrivateKeyFactory;
import org.bouncycastle.crypto.util.PublicKeyFactory;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.UUID;

public class MAuthSignatureHelper {

  public static String generateUnencryptedHeaderString(UUID appUUID, String httpVerb,
      String resourceUrl, String body, String epochTime) {
    return httpVerb + "\n" + resourceUrl + "\n" + body + "\n" + appUUID.toString() + "\n"
        + epochTime;
  }

  public static String encryptHeaderString(PrivateKey privateKey, String unencryptedString)
      throws GeneralSecurityException, IOException, CryptoException {
    String hexEncodedString = getHexEncodedDigestedString(unencryptedString);

    // encrypt
    PKCS1Encoding encryptEngine = new PKCS1Encoding(new RSAEngine());
    encryptEngine.init(true, PrivateKeyFactory.createKey(privateKey.getEncoded()));
    byte[] encryptedStringBytes = encryptEngine.processBlock(hexEncodedString.getBytes(), 0,
        hexEncodedString.getBytes().length);

    // Base64 encode
    String encryptedHeaderString = new String(Base64.encodeBase64(encryptedStringBytes), "UTF-8");

    return encryptedHeaderString;
  }

  public static String decryptSignature(PublicKey publicKey, String encryptedSignature)
      throws GeneralSecurityException, IOException, CryptoException {
    // Decode the signature from its base 64 form
    byte[] decodedSignature = Base64.decodeBase64(encryptedSignature);

    // Decrypt the signature with public key from requesting application
    PKCS1Encoding decryptEngine = new PKCS1Encoding(new RSAEngine());
    decryptEngine.init(false, PublicKeyFactory.createKey(publicKey.getEncoded()));
    byte[] decryptedHexMsg_bytes =
        decryptEngine.processBlock(decodedSignature, 0, decodedSignature.length);
    String decryptedHexSignature = new String(decryptedHexMsg_bytes);

    return decryptedHexSignature;
  }

  public static String getHexEncodedDigestedString(String unencryptedString)
      throws GeneralSecurityException {
    // Get digest
    MessageDigest md = MessageDigest.getInstance("SHA-512", "BC");
    byte[] digestedString = md.digest(unencryptedString.getBytes());

    // Convert to hex
    return Hex.encodeHexString(digestedString);
  }
}
