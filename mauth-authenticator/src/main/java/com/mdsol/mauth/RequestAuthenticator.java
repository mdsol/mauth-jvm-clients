package com.mdsol.mauth;

import com.mdsol.mauth.exception.MAuthValidationException;
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

  static {
    Security.addProvider(new BouncyCastleProvider());
  }

  public RequestAuthenticator(ClientPublicKeyProvider clientPublicKeyProvider, long requestValidationTimeoutSeconds, EpochTimeProvider epochTimeProvider) {
    this.clientPublicKeyProvider = clientPublicKeyProvider;
    this.requestValidationTimeoutSeconds = requestValidationTimeoutSeconds;
    this.epochTimeProvider = epochTimeProvider;
  }

  @Override
  public boolean authenticate(MAuthRequest mAuthRequest) {
    if (!(validateTime(mAuthRequest.getRequestTime()))) {
      final String message = "MAuth request validation failed because of timeout " + requestValidationTimeoutSeconds + "s";
      logger.error(message);
      throw new MAuthValidationException(message);
    }

    PublicKey clientPublicKey = clientPublicKeyProvider.getPublicKey(mAuthRequest.getAppUUID());

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

  // Check epoch time is not older than specified interval.
  private boolean validateTime(long requestTime) {
    long currentTime = epochTimeProvider.inSeconds();
    return (currentTime - requestTime) < requestValidationTimeoutSeconds;
  }
}
