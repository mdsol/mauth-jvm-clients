package com.mdsol.mauth.util;

import com.mdsol.mauth.MAuthRequest;
import com.mdsol.mauth.exceptions.MAuthSigningException;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang3.tuple.Pair;
import org.bouncycastle.crypto.CryptoException;

import org.bouncycastle.crypto.InvalidCipherTextException;
import org.bouncycastle.crypto.encodings.PKCS1Encoding;
import org.bouncycastle.crypto.engines.RSAEngine;
import org.bouncycastle.crypto.util.PrivateKeyFactory;
import org.bouncycastle.crypto.util.PublicKeyFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.security.*;
import java.util.Arrays;
import java.util.regex.*;
import java.util.UUID;
import java.util.stream.Collectors;

public class MAuthSignatureHelper {

  private static final Logger logger = LoggerFactory.getLogger(MAuthSignatureHelper.class);
  private static final Pattern PATTERN_HEX_LOWCASE = Pattern.compile("%[a-f0-9]{2}");

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
   * @throws IOException When failed to write to ByteArrayOutputStream
   */
  @Deprecated
  public static byte[] generateUnencryptedSignature(UUID appUUID, String httpMethod, String resourceUrl, byte[] requestBody, String epochTime) throws IOException {
    logger.debug("Generating byte[] to sign for V1");
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    String part1 = httpMethod + "\n" + resourceUrl + "\n";
    String part2 =  "\n" + appUUID.toString() + "\n" + epochTime;
    baos.write(part1.getBytes(StandardCharsets.UTF_8));
    baos.write(requestBody);
    baos.write(part2.getBytes(StandardCharsets.UTF_8));
    return baos.toByteArray();
  }

  @Deprecated
  public static String generateDigestedMessageV1(MAuthRequest mAuthRequest) throws IOException {
    logger.debug("Digest unencryptedSignature for V1");
    SequenceInputStream stream = createSequenceInputStreamV1(mAuthRequest.getAppUUID(), mAuthRequest.getHttpMethod(),
          mAuthRequest.getResourcePath(), mAuthRequest.getBodyInputStream(), String.valueOf(mAuthRequest.getRequestTime()));
    return MAuthSignatureHelper.getHexEncodedDigestedString(stream);
  }

  @Deprecated
  public static SequenceInputStream createSequenceInputStreamV1(UUID appUUID, String httpMethod, String resourceUrl, InputStream requestBody, String epochTime) {
    String part1 = httpMethod + "\n" + resourceUrl + "\n";
    InputStream part2 = requestBody;
    String part3 = "\n" + appUUID.toString() + "\n" + epochTime;
    SequenceInputStream stream = new SequenceInputStream(new ByteArrayInputStream(part1.getBytes(StandardCharsets.ISO_8859_1)),
        new SequenceInputStream(part2, new ByteArrayInputStream(part3.getBytes(StandardCharsets.ISO_8859_1))));
    return stream;
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
   *   httpMethod + "\n" + normalized_resourcePath + "\n" + requestBody_digest + "\n" + app_uuid + "\n" + epochTime + "\n" + encoded_queryParameters
   *
   * @throws MAuthSigningException when generating Unencrypted Signature errors
   */
  public static String generateStringToSignV2(UUID appUUID, String httpMethod, String resourcePath,
                                              String queryParameters, byte[] requestBody, String epochTime) throws MAuthSigningException{
    logger.debug("Generating String to sign for V2");
    String bodyDigest = getHexEncodedDigestedString(requestBody);
    return stringToSignV2(appUUID, httpMethod, resourcePath, queryParameters, bodyDigest, epochTime);
  }

  /**
   * Generate string_to_sign for Mauth V2 protocol
   * @param appUUID: application uuid
   * @param httpMethod: Http_Verb
   * @param resourcePath: resource_path (Only the path segment of the URL; first "/" is included)
   * @param queryParameters: request parameters string
   * @param requestBody: request InputStream
   * @param epochTime: current seconds since Epoch
   * @return String
   *   httpMethod + "\n" + normalized_resourcePath + "\n" + requestBody_digest + "\n" + app_uuid + "\n" + epochTime + "\n" + encoded_queryParameters
   *
   * @throws MAuthSigningException when generating Unencrypted Signature errors
   */
  public static String generateStringToSignV2(UUID appUUID, String httpMethod, String resourcePath,
                                              String queryParameters, InputStream requestBody, String epochTime) throws MAuthSigningException{
    logger.debug("Generating String to sign for V2");
    String bodyDigest = getHexEncodedDigestedString(requestBody);
    return stringToSignV2(appUUID, httpMethod, resourcePath, queryParameters, bodyDigest, epochTime);
  }

  /**
   * Generate string_to_sign for Mauth V2 protocol
   * @param mAuthRequest: Data from the incoming HTTP request
   * @return String
   *   httpMethod + "\n" + normalized_resourcePath + "\n" + requestBody_digest + "\n" + app_uuid + "\n" + epochTime + "\n" + encoded_queryParameters
   *
   * @throws MAuthSigningException when generating Unencrypted Signature errors
   */
  public static String generateStringToSignV2(MAuthRequest mAuthRequest) throws MAuthSigningException{
    logger.debug("Generating String to sign for V2");
    String epochTime = String.valueOf(mAuthRequest.getRequestTime());
    String bodyDigest = getHexEncodedDigestedString(mAuthRequest.getBodyInputStream());
    return stringToSignV2(mAuthRequest.getAppUUID(), mAuthRequest.getHttpMethod(),
        mAuthRequest.getResourcePath(), mAuthRequest.getQueryParameters(), bodyDigest, epochTime);
   }

  private static String stringToSignV2(UUID appUUID, String httpMethod,
      String resourcePath, String queryParameters, String bodyDigest, String epochTime) {
    logger.debug("Generating String to sign for V2");
    String encryptedQueryParams = generateEncryptedQueryParams(queryParameters);
    return httpMethod.toUpperCase() + "\n" + normalizePath(resourcePath) + "\n" + bodyDigest + "\n"
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
    return encryptSignaturePKCS1(privateKey, hexEncodedString);
  }

  /**
   * Generate base64 encoded signature for Mauth V1 protocol
   *
   * @deprecated
   *   This is used for Mauth V1 protocol,
   *   replaced by {@link #encryptSignatureRSA(PrivateKey privateKey, String unencryptedString)} for Mauth V2 protocol
   *
   * @param privateKey the private key of the identity whose signature is going to be generated.
   * @param unencryptedData the bytes array be signed
   * @return String of Base64 decode the digital signature
   * @throws IOException
   * @throws CryptoException
   */
  @Deprecated
  public static String encryptSignature(PrivateKey privateKey, byte[] unencryptedData) throws IOException, CryptoException {
    String hexEncodedString = getHexEncodedDigestedString(unencryptedData);
    return encryptSignaturePKCS1(privateKey, hexEncodedString);
  }

  /**
   * Generate base64 encoded signature for Mauth V1 protocol
   *
   * @deprecated
   *   This is used for Mauth V1 protocol,
   *   replaced by {@link #encryptSignatureRSA(PrivateKey privateKey, String unencryptedString)} for Mauth V2 protocol
   *
   * @param privateKey the private key of the identity whose signature is going to be generated.
   * @param inputStream the input stream be signed
   * @return String of Base64 decode the digital signature
   * @throws IOException
   * @throws CryptoException
   */
  @Deprecated
  public static String encryptSignature(PrivateKey privateKey, InputStream inputStream) throws IOException, CryptoException {
    String hexEncodedString = getHexEncodedDigestedString(inputStream);
    return encryptSignaturePKCS1(privateKey, hexEncodedString);
  }

  @Deprecated
  private static String encryptSignaturePKCS1(PrivateKey privateKey, String hexEncodedString) throws IOException, CryptoException {
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

  public static String getHexEncodedDigestedString(InputStream inputStream) {
    try {
      // Get digest
      MessageDigest md = MessageDigest.getInstance("SHA-512");
      try (DigestInputStream dis = new DigestInputStream(inputStream, md)) {
        while (dis.read() != -1) {
        }
        byte[] digestedBytes = md.digest();
        return Hex.encodeHexString(digestedBytes);
      } catch (IOException e) {
        final String message = "Invalid MessageDigestInputStream.";
        logger.error(message, e);
        throw new MAuthSigningException(message, e);
      }
    } catch (NoSuchAlgorithmException ex) {
      final String message = "Invalid algorithm or security provider.";
      logger.error(message, ex);
      throw new MAuthSigningException(message, ex);
    }
  }

  /**
   * generate the query parameters for Mauth V2
   * @param encodedQuery the encoded query string
   * @return the sorted-encoded string
   *
   * See https://learn.mdsol.com/display/CA/Building+an+mAuth-Authenticated+API
   */
  public static String generateEncryptedQueryParams(String encodedQuery) {

    if (encodedQuery == null || encodedQuery.isEmpty())
      return "";

    return Arrays.stream(encodedQuery.split("&"))
        .filter(s -> !s.isEmpty())
        .map(keyValStr -> {
          String[] split = keyValStr.split("=");
          String key = split[0];
          String value = split.length > 1 ? split[1] : "";
          return Pair.of(urlDecodeValue(key), urlDecodeValue(value));
        })
        .sorted()
        .map(keyVal -> urlEncodeValue(keyVal.getKey()) + "=" + urlEncodeValue(keyVal.getValue()))
        .collect(Collectors.joining("&"));
  }

  /**
   * encode a string using `UTF-8` encoding scheme
   * @param value
   * @return the encoded String
   *
   * See https://stackoverflow.com/questions/13060034/what-is-correct-oauth-percent-encoding
   */
  private static String urlEncodeValue(String value) {
    if (value == null ||  value.isEmpty())
      return value;

    try {
      String encodedValue = URLEncoder.encode(value, StandardCharsets.UTF_8.toString());
      // OAuth encodes some characters differently
      encodedValue = encodedValue.replace("+", "%20").replace("%7E", "~").replace("*", "%2A");
      return encodedValue;
    } catch (UnsupportedEncodingException ex) {
      throw new RuntimeException(ex.getCause());
    }
  }

  /**
   * decode a string using `UTF-8` scheme
   * @param encodedValue
   * @return the decoded String
   *
   * See https://docs.oracle.com/javase/8/docs/api/java/net/URLDecoder.html
   */
  private static String urlDecodeValue(String encodedValue) {
    if (encodedValue == null || encodedValue.isEmpty())
      return encodedValue;

    try {
      String data = encodedValue.replaceAll("%(?![0-9a-fA-F]{2})", "%25").replaceAll("\\+", " ");
      return URLDecoder.decode(data, StandardCharsets.UTF_8.toString());
    } catch (UnsupportedEncodingException ex) {
      throw new RuntimeException(ex.getCause());
    }
  }

  /**
   * normalize  url-encoded path string
   * @param encodedPath
   * @return the normalized string of path
   */
  public static String normalizePath(String encodedPath) {
    if (encodedPath == null || encodedPath.isEmpty())
      return "";

    //Normalize percent encoding to uppercase i.e. %cf%80 => %CF%80
    Matcher matcher = PATTERN_HEX_LOWCASE.matcher(encodedPath);
    StringBuffer result = new StringBuffer();
    while (matcher.find()) {
      matcher.appendReplacement(result, matcher.group().toUpperCase());
    }
    matcher.appendTail(result);

    String normalizedPath = Paths.get(result.toString()).normalize().toString();
    if(!normalizedPath.endsWith("/") &&
        (encodedPath.endsWith("/") || encodedPath.endsWith("/.")|| encodedPath.endsWith("/.."))) {
      normalizedPath = normalizedPath.concat("/");
    }
    return normalizedPath;
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
