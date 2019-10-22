package com.mdsol.mauth;

import com.mdsol.mauth.exception.MAuthValidationException;
import com.mdsol.mauth.util.CurrentEpochTimeProvider;
import com.mdsol.mauth.util.EpochTimeProvider;
import com.mdsol.mauth.util.MAuthSignatureHelper;
import com.mdsol.mauth.utils.ClientPublicKeyProvider;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.security.PublicKey;
import java.security.Security;
import java.util.Arrays;

public class RequestAuthenticator implements Authenticator {

  private static final Logger logger = LoggerFactory.getLogger(RequestAuthenticator.class);

  private static final boolean DEFAULT_DISABLE_V1 = false;

  private final ClientPublicKeyProvider clientPublicKeyProvider;
  private final long requestValidationTimeoutSeconds;
  private final EpochTimeProvider epochTimeProvider;
  private final boolean disableV1;

  static {
    Security.addProvider(new BouncyCastleProvider());
  }

  /**
   * Uses
   * 10L as default value for request validation timeout
   * {@link com.mdsol.mauth.utils.ClientPublicKeyProvider} as the EpochTimeProvider
   *
   * @param clientPublicKeyProvider PublicKey provider
   */
  public RequestAuthenticator(ClientPublicKeyProvider clientPublicKeyProvider) {
    this(clientPublicKeyProvider, 10L);
  }

  /**
   * Uses
   * 10L as default value for request validation timeout
   * {@link com.mdsol.mauth.utils.ClientPublicKeyProvider} as the EpochTimeProvider
   *
   * @param clientPublicKeyProvider PublicKey provider
   * @param disableV1
   */
  public RequestAuthenticator(ClientPublicKeyProvider clientPublicKeyProvider, boolean disableV1) {
    this(clientPublicKeyProvider, 10L, disableV1);
  }

  /**
   * Uses {@link com.mdsol.mauth.utils.ClientPublicKeyProvider} as the EpochTimeProvider
   *
   * @param clientPublicKeyProvider  PublicKey provider
   * @param requestValidationTimeoutSeconds timeout
   */
  public RequestAuthenticator(ClientPublicKeyProvider clientPublicKeyProvider, long requestValidationTimeoutSeconds) {
    this(clientPublicKeyProvider, requestValidationTimeoutSeconds, new CurrentEpochTimeProvider());
  }

  /**
   * Uses {@link com.mdsol.mauth.utils.ClientPublicKeyProvider} as the EpochTimeProvider
   *
   * @param clientPublicKeyProvider  PublicKey provider
   * @param requestValidationTimeoutSeconds timeout
   * @param disableV1
   */
  public RequestAuthenticator(ClientPublicKeyProvider clientPublicKeyProvider,
      long requestValidationTimeoutSeconds, boolean disableV1) {
    this(clientPublicKeyProvider, requestValidationTimeoutSeconds, new CurrentEpochTimeProvider(), disableV1);
  }


  public RequestAuthenticator(ClientPublicKeyProvider clientPublicKeyProvider,
      long requestValidationTimeoutSeconds, EpochTimeProvider epochTimeProvider) {
    this(clientPublicKeyProvider, requestValidationTimeoutSeconds, epochTimeProvider, DEFAULT_DISABLE_V1);
  }

  public RequestAuthenticator(ClientPublicKeyProvider clientPublicKeyProvider,
      long requestValidationTimeoutSeconds, EpochTimeProvider epochTimeProvider, boolean disableV1) {
    this.clientPublicKeyProvider = clientPublicKeyProvider;
    this.requestValidationTimeoutSeconds = requestValidationTimeoutSeconds;
    this.epochTimeProvider = epochTimeProvider;
    this.disableV1 = disableV1;
  }

  @Override
  public boolean authenticate(MAuthRequest mAuthRequest) {
    final String msgFormat = "Mauth-client attempting to authenticate request from app with mauth app uuid %s using version %s.";
    logger.info(String.format(msgFormat, mAuthRequest.getAppUUID(), mAuthRequest.getMauthVersion().getValue()));

    if (!(validateTime(mAuthRequest.getRequestTime()))) {
      final String message = "MAuth request validation failed because of timeout " + requestValidationTimeoutSeconds + "s";
      logger.error(message);
      throw new MAuthValidationException(message);
    }

    if (disableV1 && !mAuthRequest.getMauthVersion().equals(MAuthVersion.MWSV2)) {
      final  String message  = "The service requires mAuth v2 authentication headers.";
      logger.error(message);
      throw new MAuthValidationException(message);
    }

    PublicKey clientPublicKey = clientPublicKeyProvider.getPublicKey(mAuthRequest.getAppUUID());
    if (mAuthRequest.getMauthVersion().equals(MAuthVersion.MWSV2)) {
      return validateSignatureV2(mAuthRequest, clientPublicKey);
    }
    else {
      return validateSignatureV1(mAuthRequest, clientPublicKey);
    }
  }

  // Check epoch time is not older than specified interval.
  private boolean validateTime(long requestTime) {
    long currentTime = epochTimeProvider.inSeconds();
    return (currentTime - requestTime) < requestValidationTimeoutSeconds;
  }

  // check signature for V1
  private boolean validateSignatureV1 (MAuthRequest mAuthRequest, PublicKey clientPublicKey ) {

    // Decrypt the signature with public key from requesting application.
    byte[] decryptedSignature = MAuthSignatureHelper.decryptSignature(clientPublicKey, mAuthRequest.getRequestSignature());

    // Recreate the plain text signature, based on the incoming request parameters, and hash it.
    String unencryptedRequestString =
        MAuthSignatureHelper.generateUnencryptedSignature(
            mAuthRequest.getAppUUID(), mAuthRequest.getHttpMethod(), mAuthRequest.getResourcePath(),
            new String(mAuthRequest.getMessagePayload(), StandardCharsets.UTF_8),
            String.valueOf(mAuthRequest.getRequestTime())
        );
    byte[] messageDigest_bytes = MAuthSignatureHelper
        .getHexEncodedDigestedString(unencryptedRequestString).getBytes(StandardCharsets.UTF_8);

    // Compare the decrypted signature and the recreated signature hashes.
    // If both match, the request was signed by the requesting application and is valid.
    return Arrays.equals(messageDigest_bytes, decryptedSignature);
  }

  // check signature for V2
  private boolean validateSignatureV2 (MAuthRequest mAuthRequest, PublicKey clientPublicKey ) {

    // Recreate the plain text signature, based on the incoming request parameters, and hash it.
    String unencryptedRequestString =
        MAuthSignatureHelper.generateStringToSign(
            mAuthRequest.getAppUUID(), mAuthRequest.getHttpMethod(), mAuthRequest.getResourcePath(),
            mAuthRequest.getQueryParameters(),
            new String(mAuthRequest.getMessagePayload(), StandardCharsets.UTF_8),
            String.valueOf(mAuthRequest.getRequestTime()),
            MAuthVersion.MWSV2
        );

    // Compare the decrypted signature and the recreated signature hashes.
    try {
      return MAuthSignatureHelper.verifyRSA(unencryptedRequestString, mAuthRequest.getRequestSignature(), clientPublicKey);
    } catch (Exception ex) {
      final String message = "MAuth request validation failed because of " + ex.getMessage();
      logger.error(message);
      throw new MAuthValidationException(message);
    }
  }

}
