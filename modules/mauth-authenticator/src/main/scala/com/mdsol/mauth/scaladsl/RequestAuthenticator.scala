package com.mdsol.mauth.scaladsl

import java.nio.charset.StandardCharsets
import java.security.PublicKey
import java.util

import com.mdsol.mauth.MAuthRequest
import com.mdsol.mauth.MAuthVersion
import com.mdsol.mauth.exception.MAuthValidationException
import com.mdsol.mauth.scaladsl.utils.ClientPublicKeyProvider
import com.mdsol.mauth.util.{EpochTimeProvider, MAuthSignatureHelper}
import org.slf4j.LoggerFactory

import scala.concurrent.duration.Duration
import scala.concurrent.{ExecutionContext, Future, Promise}

class RequestAuthenticator(publicKeyProvider: ClientPublicKeyProvider, epochTimeProvider: EpochTimeProvider, v2OnlyAuthenticate: Boolean)
    extends Authenticator {

  private val logger = LoggerFactory.getLogger(classOf[RequestAuthenticator])

  def this(publicKeyProvider: ClientPublicKeyProvider, epochTimeProvider: EpochTimeProvider) =
    this(publicKeyProvider, epochTimeProvider, false)

  /**
    * Performs the validation of an incoming HTTP request.
    *
    * The validation process consists of recreating the mAuth hashed signature from the request data
    * and comparing it to the decrypted hash signature from the mAuth header.
    *
    * @param mAuthRequest Data from the incoming HTTP request necessary to perform the validation.
    * @return True or false indicating if the request is valid or not with respect to mAuth.
    */
  override def authenticate(mAuthRequest: MAuthRequest)(implicit ex: ExecutionContext, requestValidationTimeout: Duration): Future[Boolean] = {
    val msgFormat = "Mauth-client attempting to authenticate request from app with mauth app uuid %s using version %s."
    logger.info(String.format(msgFormat, mAuthRequest.getAppUUID, mAuthRequest.getMauthVersion.getValue))

    val promise = Promise[Boolean]()
    if (!validateTime(mAuthRequest.getRequestTime)(requestValidationTimeout)) {
      val message = s"MAuth request validation failed because of timeout $requestValidationTimeout"
      logger.error(message)
      promise.failure(new MAuthValidationException(message))
    } else if (!validateMauthVersion(mAuthRequest, v2OnlyAuthenticate)) {
      val message = "The service requires mAuth v2 authentication headers."
      logger.error(message)
      promise.failure(new MAuthValidationException(message))
    } else {
      promise.completeWith(
        publicKeyProvider.getPublicKey(mAuthRequest.getAppUUID).map {
          case None =>
            logger.error("Public Key couldn't be retrieved")
            false
          case Some(clientPublicKey) =>
            // Decrypt the signature with public key from requesting application.
            mAuthRequest.getMauthVersion match {
              case MAuthVersion.MWS =>
                validateSignatureV1(mAuthRequest, clientPublicKey)
              case MAuthVersion.MWSV2 =>
                validateSignatureV2(mAuthRequest, clientPublicKey)
            }
        }
      )
    }
    promise.future
  }

  /**
    * check if mauth v2 only authenticate is enabled or not
    * @return True or false identifying if v2 only authenticate is enabled or not.
    */
  override val isV2OnlyAuthenticate: Boolean = v2OnlyAuthenticate

  // Check epoch time is not older than specified interval.
  protected def validateTime(requestTime: Long)(requestValidationTimeout: Duration): Boolean =
    (epochTimeProvider.inSeconds - requestTime) < requestValidationTimeout.toSeconds

  // Check V2 header if only V2 is required
  protected def validateMauthVersion(mAuthRequest: MAuthRequest, v2OnlyAuthenticate: Boolean): Boolean =
    !v2OnlyAuthenticate || mAuthRequest.getMauthVersion == MAuthVersion.MWSV2

  // check signature for V1
  private def validateSignatureV1(mAuthRequest: MAuthRequest, clientPublicKey: PublicKey): Boolean = {
    val decryptedSignature = MAuthSignatureHelper.decryptSignature(clientPublicKey, mAuthRequest.getRequestSignature)
    // Recreate the plain text signature, based on the incoming request parameters, and hash it.
    try {
      val unencryptedRequestBytes = MAuthSignatureHelper.generateUnencryptedSignature(
        mAuthRequest.getAppUUID,
        mAuthRequest.getHttpMethod,
        mAuthRequest.getResourcePath,
        mAuthRequest.getMessagePayload,
        String.valueOf(mAuthRequest.getRequestTime)
      )
      val messageDigest_bytes = MAuthSignatureHelper.getHexEncodedDigestedString(unencryptedRequestBytes).getBytes(StandardCharsets.UTF_8)

      // Compare the decrypted signature and the recreated signature hashes.
      // If both match, the request was signed by the requesting application and is valid.
      util.Arrays.equals(messageDigest_bytes, decryptedSignature)
    } catch {
      case ex: Exception =>
        val message = "MAuth request validation failed because of " + ex.getMessage
        logger.error(message)
        return false
    }
  }

  // check signature for V2
  private def validateSignatureV2(mAuthRequest: MAuthRequest, clientPublicKey: PublicKey): Boolean = {
    // Recreate the plain text signature, based on the incoming request parameters, and hash it.
    val unencryptedRequestString = MAuthSignatureHelper.generateStringToSignV2(
      mAuthRequest.getAppUUID,
      mAuthRequest.getHttpMethod,
      mAuthRequest.getResourcePath,
      mAuthRequest.getQueryParameters,
      mAuthRequest.getMessagePayload,
      String.valueOf(mAuthRequest.getRequestTime)
    )

    // Compare the decrypted signature and the recreated signature hashes.
    try {
      MAuthSignatureHelper.verifyRSA(unencryptedRequestString, mAuthRequest.getRequestSignature, clientPublicKey)
    } catch {
      case ex: Exception =>
        val message = "MAuth request validation failed because of " + ex.getMessage
        logger.error(message)
        return false
    }

  }

}
