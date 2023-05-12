package com.mdsol.mauth.scaladsl

import com.mdsol.mauth.exception.MAuthValidationException
import com.mdsol.mauth.{MAuthRequest, MAuthVersion}
import com.mdsol.mauth.util.{EpochTimeProvider, MAuthSignatureHelper}
import org.slf4j.Logger

import java.nio.charset.StandardCharsets
import java.security.PublicKey
import java.util
import scala.concurrent.duration.Duration

trait Authenticator[F[_]] {

  /** Performs the validation of an incoming HTTP request.
    *
    * The validation process consists of recreating the mAuth hashed signature from the request data
    * and comparing it to the decrypted hash signature from the mAuth header.
    *
    * @param mAuthRequest Data from the incoming HTTP request necessary to perform the validation.
    * @return True or false indicating if the request is valid or not with respect to mAuth.
    */
  def authenticate(mAuthRequest: MAuthRequest)(implicit requestValidationTimeout: Duration): F[Boolean]

  /** check if mauth v2 only authenticate is enabled or not
    * @return True or false identifying if mauth v2 only is enabled or not.
    */
  def isV2OnlyAuthenticate: Boolean

  def epochTimeProvider: EpochTimeProvider

  def logger: Logger


  // Check epoch time is not older than specified interval.
  protected def validateTime(requestTime: Long)(requestValidationTimeout: Duration): Boolean =
    (epochTimeProvider.inSeconds - requestTime) < requestValidationTimeout.toSeconds

  // Check V2 header if only V2 is required
  protected def validateMauthVersion(mAuthRequest: MAuthRequest, v2OnlyAuthenticate: Boolean): Boolean =
    !v2OnlyAuthenticate || mAuthRequest.getMauthVersion == MAuthVersion.MWSV2


  // check signature for V1
  protected def validateSignatureV1(mAuthRequest: MAuthRequest, clientPublicKey: PublicKey): Boolean = {
    logAuthenticationRequest(mAuthRequest)
    val decryptedSignature = MAuthSignatureHelper.decryptSignature(clientPublicKey, mAuthRequest.getRequestSignature)
    // Recreate the plain text signature, based on the incoming request parameters, and hash it.
    try {
      val messageDigest_bytes = MAuthSignatureHelper.generateDigestedMessageV1(mAuthRequest).getBytes(StandardCharsets.UTF_8)

      // Compare the decrypted signature and the recreated signature hashes.
      // If both match, the request was signed by the requesting application and is valid.
      util.Arrays.equals(messageDigest_bytes, decryptedSignature)
    } catch {
      case ex: Exception =>
        val message = "MAuth request validation failed for V1."
        logger.error(message, ex)
        throw new MAuthValidationException(message, ex)
    }
  }


  // check signature for V2
  protected def validateSignatureV2(mAuthRequest: MAuthRequest, clientPublicKey: PublicKey): Boolean = {
    logAuthenticationRequest(mAuthRequest)
    // Recreate the plain text signature, based on the incoming request parameters, and hash it.
    val unencryptedRequestString = MAuthSignatureHelper.generateStringToSignV2(mAuthRequest)

    // Compare the decrypted signature and the recreated signature hashes.
    try MAuthSignatureHelper.verifyRSA(unencryptedRequestString, mAuthRequest.getRequestSignature, clientPublicKey)
    catch {
      case ex: Exception =>
        val message = "MAuth request validation failed for V2."
        logger.error(message, ex)
        throw new MAuthValidationException(message, ex)
    }

  }

  protected def fallbackValidateSignatureV1(mAuthRequest: MAuthRequest, clientPublicKey: PublicKey): Boolean = {
    var isValidated = false
    if (mAuthRequest.getMessagePayload == null) {
      logger.warn("V1 authentication fallback is not available because the full request body is not available in memory.")
    } else if (mAuthRequest.getXmwsSignature != null && mAuthRequest.getXmwsTime != null) {
      val mAuthRequestV1 = new MAuthRequest(
        mAuthRequest.getXmwsSignature,
        mAuthRequest.getMessagePayload,
        mAuthRequest.getHttpMethod,
        mAuthRequest.getXmwsTime,
        mAuthRequest.getResourcePath,
        mAuthRequest.getQueryParameters
      )
      isValidated = validateSignatureV1(mAuthRequestV1, clientPublicKey)
      if (isValidated) {
        logger.warn("Completed successful authentication attempt after fallback to V1")
      }
    }
    isValidated
  }


  private def logAuthenticationRequest(mAuthRequest: MAuthRequest): Unit = {
    val msgFormat = "Mauth-client attempting to authenticate request from app with mauth app uuid %s using version %s."
    logger.info(String.format(msgFormat, mAuthRequest.getAppUUID, mAuthRequest.getMauthVersion.getValue))
  }

}
