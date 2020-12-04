package com.mdsol.mauth;

import com.mdsol.mauth.exceptions.MAuthSigningException;
import com.mdsol.mauth.util.CurrentEpochTimeProvider;
import com.mdsol.mauth.util.EpochTimeProvider;
import com.mdsol.mauth.util.MAuthHeadersHelper;
import com.mdsol.mauth.util.MAuthSignatureHelper;
import org.bouncycastle.crypto.CryptoException;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.security.Security;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.mdsol.mauth.util.MAuthKeysHelper.getPrivateKeyFromString;

public class DefaultSigner implements Signer {
  private static final Logger logger = LoggerFactory.getLogger(DefaultSigner.class);

  private final UUID appUUID;
  private final PrivateKey privateKey;
  private final EpochTimeProvider epochTimeProvider;
  private List<MAuthVersion> signVersions;

  static {
    Security.addProvider(new BouncyCastleProvider());
  }

  public DefaultSigner(SignerConfiguration configuration) {
    this(configuration.getAppUUID(), getPrivateKeyFromString(configuration.getPrivateKey()),
        new CurrentEpochTimeProvider(), configuration.getSignVersions());
  }

  public DefaultSigner(UUID appUUID, String privateKey) {
    this(appUUID, getPrivateKeyFromString(privateKey));
  }

  public DefaultSigner(UUID appUUID, String privateKey, EpochTimeProvider epochTimeProvider) {
    this(appUUID, getPrivateKeyFromString(privateKey), epochTimeProvider);
  }

  public DefaultSigner(UUID appUUID, PrivateKey privateKey) {
    this(appUUID, privateKey, new CurrentEpochTimeProvider());
  }

  public DefaultSigner(UUID appUUID, PrivateKey privateKey, EpochTimeProvider epochTimeProvider) {
    this(appUUID, privateKey, epochTimeProvider, SignerConfiguration.DEFAULT_SIGN_VERSION);
  }

  public DefaultSigner(UUID appUUID, String privateKey, EpochTimeProvider epochTimeProvider, List<MAuthVersion> signVersions) {
    this(appUUID, getPrivateKeyFromString(privateKey), epochTimeProvider, signVersions);
  }

  public DefaultSigner(UUID appUUID, PrivateKey privateKey, EpochTimeProvider epochTimeProvider, List<MAuthVersion> signVersions) {
    this.appUUID = appUUID;
    this.privateKey = privateKey;
    this.epochTimeProvider = epochTimeProvider;
    this.signVersions = signVersions == null || signVersions.isEmpty() ? SignerConfiguration.DEFAULT_SIGN_VERSION : signVersions;
  }

  @Override
  @Deprecated
  public Map<String, String> generateRequestHeaders(
      String httpVerb, String requestPath, String requestPayload) throws MAuthSigningException {
    if (null == requestPayload) {
      requestPayload = "";
    }
    // mAuth uses an epoch time measured in seconds
    long currentTime = epochTimeProvider.inSeconds();

    return generateRequestHeadersV1(httpVerb, requestPath, requestPayload.getBytes(StandardCharsets.UTF_8), currentTime);
  }

  @Override
  public Map<String, String> generateRequestHeaders(
      String httpVerb, String requestPath, byte[] requestPayload, String queryParameters) throws MAuthSigningException {

    if (null == requestPayload) {
      requestPayload = "".getBytes(StandardCharsets.UTF_8);
    }
    if (null == queryParameters) {
      queryParameters = "";
    }

    // mAuth uses an epoch time measured in seconds
    long currentTime = epochTimeProvider.inSeconds();

    HashMap<String, String> headers = new HashMap<>();
    if (signVersions.contains(MAuthVersion.MWS)) {
      // Add v1 headers if v1 is in sign version list
      Map<String, String> headersV1 = generateRequestHeadersV1(httpVerb, requestPath, requestPayload, currentTime);
      if (!headersV1.isEmpty()) {
        headers.putAll(headersV1);
      }
    }

    if (signVersions.contains(MAuthVersion.MWSV2)) {
      // Add v2 headers if v2 is in sign version list
      Map<String, String> headersV2 = generateRequestHeadersV2(httpVerb, requestPath, queryParameters, requestPayload, currentTime);
      if (!headersV2.isEmpty()) {
        headers.putAll(headersV2);
      }
    }
    return headers;
  }

  @Override
  public Map<String, String> generateRequestHeaders(
      String httpVerb, String requestPath, InputStream requestPayload, String queryParameters) throws MAuthSigningException {
    if (null == requestPayload) {
      return generateRequestHeaders(httpVerb, requestPath, "".getBytes(StandardCharsets.UTF_8), queryParameters);
    }

    if (null == queryParameters) {
      queryParameters = "";
    }

    // mAuth uses an epoch time measured in seconds
    long currentTime = epochTimeProvider.inSeconds();

    HashMap<String, String> headers = new HashMap<>();
    if (signVersions.contains(MAuthVersion.MWSV2)) {
      // Add v2 headers if v2 is in sign version list
      Map<String, String> headersV2 = generateRequestHeadersV2(httpVerb, requestPath, queryParameters, requestPayload, currentTime);
      if (!headersV2.isEmpty()) {
        headers.putAll(headersV2);
      }
    } else {
      // Add v1 headers
      Map<String, String> headersV1 = generateRequestHeadersV1(httpVerb, requestPath, requestPayload, currentTime);
      if (!headersV1.isEmpty()) {
        headers.putAll(headersV1);
      }
    }

    return headers;
  }

  private Map<String, String> generateRequestHeadersV1(
      String httpVerb, String requestPath, byte[] requestPayload, long currentTime) throws MAuthSigningException {

    String encryptedSignature;
    try {
      byte[] unencryptedSignature = MAuthSignatureHelper.generateUnencryptedSignature(
          appUUID, httpVerb, requestPath, requestPayload, String.valueOf(currentTime));

      encryptedSignature = MAuthSignatureHelper.encryptSignature(privateKey, unencryptedSignature);
    } catch (IOException | CryptoException e) {
      logger.error("Error generating request headers", e);
      throw new MAuthSigningException(e);
    }

    return generateRequestHeadersV1(encryptedSignature, currentTime);
  }

  private Map<String, String> generateRequestHeadersV1(
      String httpVerb, String requestPath, InputStream requestPayload, long currentTime) throws MAuthSigningException {

    String encryptedSignature;
    try {
      SequenceInputStream stream = MAuthSignatureHelper.createSequenceInputStreamV1(
          appUUID, httpVerb, requestPath, requestPayload, String.valueOf(currentTime));

      encryptedSignature = MAuthSignatureHelper.encryptSignature(privateKey, stream);
    } catch (IOException | CryptoException e) {
      logger.error("Error generating request headers", e);
      throw new MAuthSigningException(e);
    }

    return generateRequestHeadersV1(encryptedSignature, currentTime);
  }

  private Map<String, String> generateRequestHeadersV1(String encryptedSignature, long currentTime) {
    HashMap<String, String> headers = new HashMap<>();
    headers.put(
        MAuthRequest.X_MWS_AUTHENTICATION_HEADER_NAME,
        MAuthHeadersHelper.createAuthenticationHeaderValue(appUUID, encryptedSignature)
    );
    headers.put(MAuthRequest.X_MWS_TIME_HEADER_NAME, MAuthHeadersHelper.createTimeHeaderValue(currentTime));
    return headers;
  }

  private Map<String, String> generateRequestHeadersV2(String httpVerb, String requestPath, String queryParameters,
      byte[] requestPayload, long currentTime) throws MAuthSigningException {
     String stringToSign = MAuthSignatureHelper.generateStringToSignV2(
        appUUID, httpVerb, requestPath, queryParameters, requestPayload, String.valueOf(currentTime));
    return generateRequestHeadersV2(stringToSign, currentTime);
  }

  private Map<String, String> generateRequestHeadersV2(String httpVerb, String requestPath, String queryParameters,
      InputStream requestPayload, long currentTime) throws MAuthSigningException {
    String stringToSign = MAuthSignatureHelper.generateStringToSignV2(
        appUUID, httpVerb, requestPath, queryParameters, requestPayload, String.valueOf(currentTime));
    return generateRequestHeadersV2(stringToSign, currentTime);
  }

  private Map<String, String> generateRequestHeadersV2(String stringToSign, long currentTime) throws MAuthSigningException {
    String encryptedSignature;
    try {
      encryptedSignature = MAuthSignatureHelper.encryptSignatureRSA(privateKey, stringToSign);
    } catch (Exception e) {
      logger.error("Error generating request headers for V2", e);
      throw new MAuthSigningException(e);
    }

    HashMap<String, String> headers = new HashMap<>();
    headers.put(
        MAuthRequest.MCC_AUTHENTICATION_HEADER_NAME,
        MAuthHeadersHelper.createAuthenticationHeaderValue(appUUID, encryptedSignature, MAuthVersion.MWSV2.getValue())
    );
    headers.put(MAuthRequest.MCC_TIME_HEADER_NAME, MAuthHeadersHelper.createTimeHeaderValue(currentTime));

    return headers;
  }

}
