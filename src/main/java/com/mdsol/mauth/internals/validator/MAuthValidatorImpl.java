package com.mdsol.mauth.internals.validator;

import com.mdsol.mauth.domain.MAuthRequest;
import com.mdsol.mauth.exceptions.MAuthValidationException;
import com.mdsol.mauth.internals.client.MAuthClient;
import com.mdsol.mauth.internals.utils.EpochTime;
import com.mdsol.mauth.internals.utils.MAuthSignatureHelper;

import java.nio.charset.StandardCharsets;
import java.security.PublicKey;
import java.util.Arrays;

public class MAuthValidatorImpl implements MAuthValidator {

  private final long REQUEST_VALIDATION_TIMEOUT = 300L; // 5 minutes

  private final MAuthClient mAuthClient;
  private final EpochTime epochTime;

  public MAuthValidatorImpl(MAuthClient mAuthClient, EpochTime epochTime) {
    this.mAuthClient = mAuthClient;
    this.epochTime = epochTime;
  }

  @Override
  public boolean validate(MAuthRequest mAuthRequest) {
    if (!(validateTime(mAuthRequest.getRequestTime()))) {
      throw new MAuthValidationException(
          "MAuth request validation failed because of timeout " + REQUEST_VALIDATION_TIMEOUT + "s");
    }

    PublicKey publicKey = mAuthClient.getPublicKey(mAuthRequest.getAppUUID());

    // Decrypt the signature with public key from requesting application.
    byte[] decryptedSignature =
        MAuthSignatureHelper.decryptSignature(publicKey, mAuthRequest.getRequestSignature());

    // Recreate the plain text signature, based on the incoming request parameters, and hash it.
    String unencryptedRequestString = MAuthSignatureHelper.generateUnencryptedSignature(
        mAuthRequest.getAppUUID(), mAuthRequest.getHttpMethod(), mAuthRequest.getResourcePath(),
        new String(mAuthRequest.getMessagePayload(), StandardCharsets.UTF_8),
        String.valueOf(mAuthRequest.getRequestTime()));
    byte[] messageDigest_bytes = MAuthSignatureHelper
        .getHexEncodedDigestedString(unencryptedRequestString).getBytes(StandardCharsets.UTF_8);

    // Compare the decrypted signature and the recreated signature hashes.
    // If both match, the request was signed by the requesting application and is valid.
    return Arrays.equals(messageDigest_bytes, decryptedSignature);
  }

  // Check epoch time is not older than specified interval.
  private boolean validateTime(long requestTime) {
    long currentTime = epochTime.getSeconds();
    return (currentTime - requestTime) < REQUEST_VALIDATION_TIMEOUT;
  }
}
