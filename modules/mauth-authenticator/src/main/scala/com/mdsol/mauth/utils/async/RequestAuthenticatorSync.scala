package com.mdsol.mauth.utils.async

import java.nio.charset.StandardCharsets
import java.security.PublicKey
import java.util
import java.util.Arrays

import com.mdsol.mauth.exception.MAuthValidationException
import com.mdsol.mauth.util.MAuthSignatureHelper
import com.mdsol.mauth.{Authenticator, MAuthRequest}

class RequestAuthenticatorSync(publicKeyProvider: ClientPublicKeyProviderAsync) extends Authenticator {
  /**
    * Performs the validation of an incoming HTTP request.
    *
    * The validation process consists of recreating the mAuth hashed signature from the request data
    * and comparing it to the decrypted hash signature from the mAuth header.
    *
    * @param mAuthRequest Data from the incoming HTTP request necessary to perform the validation.
    * @return True or false indicating if the request is valid or not with respect to mAuth.
    */
  override def authenticate(mAuthRequest: MAuthRequest): Boolean = {
    if (!validateTime(mAuthRequest.getRequestTime)) {
      val message = "MAuth request validation failed because of timeout " + requestValidationTimeoutSeconds + "s"
      logger.error(message)
      throw new MAuthValidationException(message)
    }

    val clientPublicKey = clientPublicKeyProvider.getPublicKey(mAuthRequest.getAppUUID)

    // Decrypt the signature with public key from requesting application.
    val decryptedSignature = MAuthSignatureHelper.decryptSignature(clientPublicKey, mAuthRequest.getRequestSignature)

    // Recreate the plain text signature, based on the incoming request parameters, and hash it.
    val unencryptedRequestString = MAuthSignatureHelper.generateUnencryptedSignature(mAuthRequest.getAppUUID, mAuthRequest.getHttpMethod, mAuthRequest.getResourcePath, new String(mAuthRequest.getMessagePayload, StandardCharsets.UTF_8), String.valueOf(mAuthRequest.getRequestTime))
    val messageDigest_bytes = MAuthSignatureHelper.getHexEncodedDigestedString(unencryptedRequestString).getBytes(StandardCharsets.UTF_8)

    // Compare the decrypted signature and the recreated signature hashes.
    // If both match, the request was signed by the requesting application and is valid.
    return util.Arrays.equals(messageDigest_bytes, decryptedSignature)
  }

  // Check epoch time is not older than specified interval.
  private def validateTime(requestTime: Long) = {
    val currentTime = epochTimeProvider.inSeconds
    (currentTime - requestTime) < requestValidationTimeoutSeconds
  }
}
