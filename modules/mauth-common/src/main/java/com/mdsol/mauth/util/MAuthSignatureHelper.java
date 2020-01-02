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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.Arrays;
import java.util.UUID;

public class MAuthSignatureHelper {

  private static final Logger logger = LoggerFactory.getLogger(MAuthSignatureHelper.class);

  /**
   * Generate string_to_sign for Mauth V1 protocol
   *
   * @deprecated
   *   This is used for Mauth V1 protocol,
   *   replaced by {@link #generateStringToSignV2(UUID appUUID, String httpMethod, String resourceUrl,
   *       String queryParameters, byte[] requestBody, String epochTime)} for Mauth V2 protocol
   *
   * @param appUUID: app uuid
   * @param httpMethod: Http_Verb
   * @param resourceUrl: resource_url_path (no host, port or query string; first "/" is included)
   * @param requestBody: request body string
   * @param epochTime: current seconds since Epoch
   * @return String
   *   httpMethod + "\n" + resourceUrl + "\n" + requestBody + "\n" + app_uuid + "\n" + epochTime
   *
   */
  @Deprecated
  public static String generateUnencryptedSignature(UUID appUUID, String httpMethod, String resourceUrl, String requestBody, String epochTime) {
    logger.debug("Generating String to sign for V1");
    return httpMethod + "\n" + resourceUrl + "\n" + requestBody + "\n" + appUUID.toString() + "\n" + epochTime;
  }

  /**
   * Generate byte_arrary_to_sign for Mauth V1 protocol
   *
   * @deprecated
   *   This is used for Mauth V1 protocol,
   *   replaced by {@link #generateStringToSignV2(UUID appUUID, String httpMethod, String resourceUrl,
   *       String queryParameters, byte[] requestBody, String epochTime)} for Mauth V2 protocol
   *
   * @param appUUID: app uuid
   * @param httpMethod: Http_Verb
   * @param resourceUrl: resource_url_path (no host, port or query string; first "/" is included)
   * @param requestBody: request body byte[]
   * @param epochTime: current seconds since Epoch
   * @return byte[]
   *   httpMethod + "\n" + resourceUrl + "\n" + requestBody + "\n" + app_uuid + "\n" + epochTime
   *
   */
  @Deprecated
  public static byte[] generateUnencryptedSignature(UUID appUUID, String httpMethod, String resourceUrl, byte[] requestBody, String epochTime) throws IOException{
    logger.debug("Generating byte[] to sign for V1");
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    String part1 = httpMethod + "\n" + resourceUrl + "\n";
    String part2 =  "\n" + appUUID.toString() + "\n" + epochTime;
    baos.write(part1.getBytes());
    baos.write(requestBody);
    baos.write(part2.getBytes());
    return baos.toByteArray();
  }

  /**
   * Generate string_to_sign for Mauth V2 protocol
   * @param appUUID: application uuid
   * @param httpMethod: Http_Verb
   * @param resourcePath: resource_path (Only the path segment of the URL; first "/" is included)
   * @param queryParameters: request parameters string
   * @param requestBody: request body byte[]
   * @param epochTime: current seconds since Epoch
   * @return String
   *   httpMethod + "\n" + resourcePath + "\n" + requestBody_digest + "\n" + app_uuid + "\n" + epochTime + "\n" + encoded_queryParameters
   *
   * @throws MAuthSigningException
   */
  public static String generateStringToSignV2(UUID appUUID, String httpMethod, String resourcePath,
      String queryParameters, byte[] requestBody, String epochTime) throws MAuthSigningException{
    logger.debug("Generating String to sign for V2");

    String bodyDigest;
    String encryptedQueryParams;
    try {
      bodyDigest = getHexEncodedDigestedString(requestBody);
      encryptedQueryParams = generateEncryptedQueryParams(queryParameters);
    } catch (IOException e) {
      logger.error("Error generating Unencrypted Signature", e);
      throw new MAuthSigningException(e);
    }
    return httpMethod.toUpperCase() + "\n" + resourcePath + "\n" + bodyDigest + "\n"
        + appUUID.toString() + "\n" + epochTime + "\n" + encryptedQueryParams;
  }

  /**
   * Generate base64 encoded signature for Mauth V1 protocol
   *
   * @deprecated
   *   This is used for Mauth V1 protocol,
   *   replaced by {@link #encryptSignatureRSA(PrivateKey privateKey, String unencryptedString)} for Mauth V2 protocol
   *
   * @param privateKey the private key of the identity whose signature is going to be generated.
   * @param unencryptedString the string be signed
   * @return String of Base64 decode the digital signature
   * @throws IOException
   * @throws CryptoException
   */
  @Deprecated
  public static String encryptSignature(PrivateKey privateKey, String unencryptedString) throws IOException, CryptoException {
    String hexEncodedString = getHexEncodedDigestedString(unencryptedString);

    PKCS1Encoding encryptEngine = new PKCS1Encoding(new RSAEngine());
    encryptEngine.init(true, PrivateKeyFactory.createKey(privateKey.getEncoded()));
    byte[] encryptedStringBytes = encryptEngine.processBlock(hexEncodedString.getBytes(), 0, hexEncodedString.getBytes().length);

    return new String(Base64.encodeBase64(encryptedStringBytes), StandardCharsets.UTF_8);
  }

  /**
   * Decrypt the encrypted signature for Mauth V1 protocol
   *
   * @deprecated
   *   This is used for Mauth V1 protocol,
   *   replaced by {@link #verifyRSA(String plainText, String signature, PublicKey publicKey)} for Mauth V2 protocol
   *
   * @param publicKey he public key of the identity whose signature is going to be verified.
   * @param encryptedSignature the signature to be decrypted.
   * @return byte[] decrypted signature
   * @throws MAuthSigningException
   */
  @Deprecated
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
    byte[] unencryptedData = unencryptedString.getBytes(StandardCharsets.UTF_8);
    return getHexEncodedDigestedString(unencryptedData);
  }

  public static String getHexEncodedDigestedString(byte[] unencryptedData) {
    try {
      // Get digest
      MessageDigest md = MessageDigest.getInstance("SHA-512");
      byte[] digestedString = md.digest(unencryptedData);
      // Convert to hex
      return Hex.encodeHexString(digestedString);
    } catch (NoSuchAlgorithmException ex) {
      final String message = "Invalid algorithm or security provider.";
      logger.error(message, ex);
      throw new MAuthSigningException(message, ex);
    }
  }

  public static String generateEncryptedQueryParams(String query) throws IOException {

    if (query == null || query.isEmpty())
      return "";

    StringBuilder encryptedQueryParams = new StringBuilder();

    String[] params = query.split("&");
    Arrays.sort(params);
    for (String param : params)
    {
      String [] keyPair = param.split("=");
      if (keyPair.length > 0) {
        String name = param.split("=")[0];
        String value = keyPair.length > 1 ? param.split("=")[1] : "";
        if (encryptedQueryParams.length() > 0) {
          encryptedQueryParams.append('&');
        }
        encryptedQueryParams.append(urlEncodeValue(name)).append('=').append(urlEncodeValue(value));
      }
    }

    return encryptedQueryParams.toString();
  }

  /**
   * encode a string value using `UTF-8` encoding scheme
   * @param value
   * @return encoded String
   *
   * See https://stackoverflow.com/questions/10786042/java-url-encoding-of-query-string-parameters
   */
  private static String urlEncodeValue(String value) {
    if (value == null ||  value.isEmpty())
      return value;

    try {
      String encodedValue = URLEncoder.encode(value, StandardCharsets.UTF_8.toString());
      encodedValue = encodedValue.replace("+", "%20");
      encodedValue = encodedValue.replace("%7E", "~");
      encodedValue = encodedValue.replace("*", "%2A");
      return encodedValue;
    } catch (UnsupportedEncodingException ex) {
      throw new RuntimeException(ex.getCause());
    }
  }

  /**
   * Generate base64 encoded signature using SHA516 with RSA
   * @param privateKey the private key of the identity whose signature is going to be generated.
   * @param unencryptedString the string be signed
   * @return String of Base64 decode the digital signature
   * @throws InvalidKeyException
   * @throws NoSuchAlgorithmException
   * @throws SignatureException
   */
  public static String encryptSignatureRSA(PrivateKey privateKey, String unencryptedString)
      throws InvalidKeyException, NoSuchAlgorithmException, SignatureException {
    Signature signature = Signature.getInstance("SHA512WithRSA");
    signature.initSign(privateKey);
    signature.update(unencryptedString.getBytes(StandardCharsets.UTF_8));
    return new String(Base64.encodeBase64(signature.sign()), StandardCharsets.UTF_8);
  }

  /**
   * Verify SHA512-RSA signature
   * @param plainText the string be verified
   * @param signature the signature to be verified.
   * @param publicKey he public key of the identity whose signature is going to be verified.
   * @return boolean
   * @throws Exception
   */
  public static boolean verifyRSA(String plainText, String signature, PublicKey publicKey) throws Exception {
    Signature publicSignature = Signature.getInstance("SHA512withRSA");
    publicSignature.initVerify(publicKey);
    publicSignature.update(plainText.getBytes(StandardCharsets.UTF_8));
    byte[] signatureBytes = Base64.decodeBase64(signature.getBytes());
    return publicSignature.verify(signatureBytes);
  }

}
