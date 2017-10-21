package com.mdsol.mauth.scaladsl.utils

import java.nio.charset.StandardCharsets
import java.util

import com.mdsol.mauth.MAuthRequest
import com.mdsol.mauth.scaladsl.Authenticator
import com.mdsol.mauth.util.MAuthSignatureHelper

import scala.concurrent.Future

class RequestAuthenticator(publicKeyProvider: ClientPublicKeyProvider) extends Authenticator {
  /**
    * Performs the validation of an incoming HTTP request.
    *
    * The validation process consists of recreating the mAuth hashed signature from the request data
    * and comparing it to the decrypted hash signature from the mAuth header.
    *
    * @param mAuthRequest Data from the incoming HTTP request necessary to perform the validation.
    * @return True or false indicating if the request is valid or not with respect to mAuth.
    */
  override def authenticate(mAuthRequest: MAuthRequest): Future[Boolean] = {
    //    if (!validateTime(mAuthRequest.getRequestTime)) {
    //      val message = "MAuth request validation failed because of timeout " + requestValidationTimeoutSeconds + "s"
    //     logger.error(message)
    //      throw new MAuthValidationException(message)
    //    }

    publicKeyProvider.getPublicKey(mAuthRequest.getAppUUID).map {
      case None =>
        //        logger.error("Public Key couldn't be retrieved")
        false
      case Some(clientPublicKey) =>
        // Decrypt the signature with public key from requesting application.
        val decryptedSignature = MAuthSignatureHelper.decryptSignature(clientPublicKey, mAuthRequest.getRequestSignature)

        // Recreate the plain text signature, based on the incoming request parameters, and hash it.
        val unencryptedRequestString = MAuthSignatureHelper.generateUnencryptedSignature(mAuthRequest.getAppUUID, mAuthRequest.getHttpMethod, mAuthRequest.getResourcePath, new String(mAuthRequest.getMessagePayload, StandardCharsets.UTF_8), String.valueOf(mAuthRequest.getRequestTime))
        val messageDigest_bytes = MAuthSignatureHelper.getHexEncodedDigestedString(unencryptedRequestString).getBytes(StandardCharsets.UTF_8)

        // Compare the decrypted signature and the recreated signature hashes.
        // If both match, the request was signed by the requesting application and is valid.
        util.Arrays.equals(messageDigest_bytes, decryptedSignature)
    }
  }

  // Check epoch time is not older than specified interval.
  private def validateTime(requestTime: Long) = {
    //    val currentTime = epochTimeProvider.inSeconds
    //    (currentTime - requestTime) < requestValidationTimeoutSeconds
  }
}
