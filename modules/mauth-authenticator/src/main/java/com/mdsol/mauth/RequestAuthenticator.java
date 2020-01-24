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

  private final ClientPublicKeyProvider clientPublicKeyProvider;
  private final long requestValidationTimeoutSeconds;
  private final EpochTimeProvider epochTimeProvider;
  private final boolean v2OnlyAuthenticate;

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
   * @param v2OnlyAuthenticate the flag to identify authenticate with protocol V2 only or not,
   *                  if true, clients will authenticate with protocol V2,
   *                  if false, clients will authenticate with only the highest protocol version (V2 or V1)
   */
  public RequestAuthenticator(ClientPublicKeyProvider clientPublicKeyProvider, boolean v2OnlyAuthenticate) {
    this(clientPublicKeyProvider, 10L, v2OnlyAuthenticate);
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
   * @param v2OnlyAuthenticate the flag to identify authenticate with protocol V2 only or not,
   *                  if true, clients will authenticate with protocol V2,
   *                  if false, clients will authenticate with only the highest protocol version (V2 or V1)
   */
  public RequestAuthenticator(ClientPublicKeyProvider clientPublicKeyProvider,
      long requestValidationTimeoutSeconds, boolean v2OnlyAuthenticate) {
    this(clientPublicKeyProvider, requestValidationTimeoutSeconds, new CurrentEpochTimeProvider(), v2OnlyAuthenticate);
  }

  public RequestAuthenticator(ClientPublicKeyProvider clientPublicKeyProvider,
      long requestValidationTimeoutSeconds, EpochTimeProvider epochTimeProvider) {
    this(clientPublicKeyProvider, requestValidationTimeoutSeconds, epochTimeProvider, false);
  }

  public RequestAuthenticator(ClientPublicKeyProvider clientPublicKeyProvider,
      long requestValidationTimeoutSeconds, EpochTimeProvider epochTimeProvider, boolean v2OnlyAuthenticate) {
    this.clientPublicKeyProvider = clientPublicKeyProvider;
    this.requestValidationTimeoutSeconds = requestValidationTimeoutSeconds;
    this.epochTimeProvider = epochTimeProvider;
    this.v2OnlyAuthenticate = v2OnlyAuthenticate;
  }

  @Override
  public boolean authenticate(MAuthRequest mAuthRequest) {

    if (!(validateTime(mAuthRequest.getRequestTime()))) {
      final String message = "MAuth request validation failed because of timeout " + requestValidationTimeoutSeconds + "s";
      logger.error(message);
      throw new MAuthValidationException(message);
    }

    if (v2OnlyAuthenticate && !mAuthRequest.getMauthVersion().equals(MAuthVersion.MWSV2)) {
      final String message  = "The service requires mAuth v2 authentication headers.";
      logger.error(message);
      throw new MAuthValidationException(message);
    }

    PublicKey clientPublicKey = clientPublicKeyProvider.getPublicKey(mAuthRequest.getAppUUID());
    if (mAuthRequest.getMauthVersion().equals(MAuthVersion.MWSV2)) {
      boolean v2IsValidated = validateSignatureV2(mAuthRequest, clientPublicKey);
      if (v2OnlyAuthenticate) {
        return v2IsValidated;
      }
      else if (v2IsValidated) {
        return v2IsValidated;
      }
      else {
        return fallbackValidateSignatureV1(mAuthRequest, clientPublicKey);
      }
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

    logAuthenticationRequest(mAuthRequest);

    // Decrypt the signature with public key from requesting application.
    byte[] decryptedSignature = MAuthSignatureHelper.decryptSignature(clientPublicKey, mAuthRequest.getRequestSignature());

    // Recreate the plain text signature, based on the incoming request parameters, and hash it.
    try {
      byte[] messageDigest_bytes = MAuthSignatureHelper.generateUnencryptedSignature(
          mAuthRequest.getAppUUID(), mAuthRequest.getHttpMethod(), mAuthRequest.getResourcePath(),
          mAuthRequest.getMessagePayload(),
          String.valueOf(mAuthRequest.getRequestTime())
      );
      messageDigest_bytes = MAuthSignatureHelper.getHexEncodedDigestedString(messageDigest_bytes).getBytes(StandardCharsets.UTF_8);

      // Compare the decrypted signature and the recreated signature hashes.
      // If both match, the request was signed by the requesting application and is valid.
      return Arrays.equals(messageDigest_bytes, decryptedSignature);
    } catch (Exception ex) {
      final String message = "MAuth request validation failed for V1";
      logger.error(message, ex);
      throw new MAuthValidationException(message, ex);
    }
  }

  // check signature for V2
  private boolean validateSignatureV2 (MAuthRequest mAuthRequest, PublicKey clientPublicKey ) {

    logAuthenticationRequest(mAuthRequest);

    // Recreate the plain text signature, based on the incoming request parameters, and hash it.
    String unencryptedRequestString =
        MAuthSignatureHelper.generateStringToSignV2(
            mAuthRequest.getAppUUID(), mAuthRequest.getHttpMethod(), mAuthRequest.getResourcePath(),
            mAuthRequest.getQueryParameters(),
            mAuthRequest.getMessagePayload(),
            String.valueOf(mAuthRequest.getRequestTime())
        );

    // Compare the decrypted signature and the recreated signature hashes.
    try {
      return MAuthSignatureHelper.verifyRSA(unencryptedRequestString, mAuthRequest.getRequestSignature(), clientPublicKey);
    } catch (Exception ex) {
      final String message = "MAuth request validation failed for V2";
      logger.error(message, ex);
      throw new MAuthValidationException(message, ex);
    }
  }

  private boolean fallbackValidateSignatureV1(MAuthRequest mAuthRequest, PublicKey clientPublicKey) {
    boolean isValidated = false;
    if (mAuthRequest.getXmwsSignature() != null && mAuthRequest.getXmwsTime() != null) {
      MAuthRequest mAuthRequestV1 = new MAuthRequest(
          mAuthRequest.getXmwsSignature(),
          mAuthRequest.getMessagePayload(),
          mAuthRequest.getHttpMethod(),
          mAuthRequest.getXmwsTime(),
          mAuthRequest.getResourcePath(),
          mAuthRequest.getQueryParameters()
      );
      isValidated = validateSignatureV1(mAuthRequestV1, clientPublicKey);
      if (isValidated) {
        logger.warn("Completed successful authentication attempt after fallback to V1");
      }
    }
    return isValidated;
  }

  private void logAuthenticationRequest(MAuthRequest mAuthRequest) {
    final String msgFormat = "Mauth-client attempting to authenticate request from app with mauth app uuid %s using version %s.";
    logger.info(String.format(msgFormat, mAuthRequest.getAppUUID(), mAuthRequest.getMauthVersion().getValue()));
  }

}
