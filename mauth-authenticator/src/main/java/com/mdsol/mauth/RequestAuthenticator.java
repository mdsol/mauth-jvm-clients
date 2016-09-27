package com.mdsol.mauth;

import com.mdsol.mauth.exception.MAuthValidationException;
import com.mdsol.mauth.util.MAuthSignatureHelper;
import com.mdsol.mauth.utils.PublicKeyProvider;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.nio.charset.StandardCharsets;
import java.security.PublicKey;
import java.security.Security;
import java.util.Arrays;

public class RequestAuthenticator implements Authenticator {

  private final PublicKeyProvider publicKeyProvider;
  private final long requestValidationTimeoutSeconds;

  static {
    Security.addProvider(new BouncyCastleProvider());
  }

  public RequestAuthenticator(PublicKeyProvider publicKeyProvider, long requestValidationTimeout) {
    this.publicKeyProvider = publicKeyProvider;
    this.requestValidationTimeoutSeconds = requestValidationTimeout;
  }

  @Override
  public boolean authenticate(MAuthRequest mAuthRequest) {
    if (!(validateTime(mAuthRequest.getRequestTime()))) {
      throw new MAuthValidationException(
          "MAuth request validation failed because of timeout " + requestValidationTimeoutSeconds + "s");
    }

    PublicKey clientPublicKey = publicKeyProvider.getPublicKey(mAuthRequest.getAppUUID());

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
    long currentTime = System.currentTimeMillis() / 1000;
    return (currentTime - requestTime) < requestValidationTimeoutSeconds;
  }
}
