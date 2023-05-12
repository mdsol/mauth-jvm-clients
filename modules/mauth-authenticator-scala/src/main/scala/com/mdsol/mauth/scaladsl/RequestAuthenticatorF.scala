package com.mdsol.mauth.scaladsl

import cats.effect.Async
import com.mdsol.mauth.MAuthRequest
import com.mdsol.mauth.MAuthVersion
import com.mdsol.mauth.exception.MAuthValidationException
import com.mdsol.mauth.scaladsl.utils.ClientPublicKeyProvider
import com.mdsol.mauth.util.EpochTimeProvider
import org.slf4j.{Logger, LoggerFactory}

import scala.concurrent.duration._
import cats.implicits._

class RequestAuthenticatorF[F[_]](
  val publicKeyProvider: ClientPublicKeyProvider[F],
  override val epochTimeProvider: EpochTimeProvider,
  v2OnlyAuthenticate: Boolean
)(implicit F: Async[F])
    extends Authenticator[F] {

  val logger: Logger = LoggerFactory.getLogger(classOf[RequestAuthenticator])
  def message(requestValidationTimeout: Duration) = s"MAuth request validation failed because of timeout $requestValidationTimeout"
  val message = "The service requires mAuth v2 authentication headers."
  def this(publicKeyProvider: ClientPublicKeyProvider[F], epochTimeProvider: EpochTimeProvider)(implicit F: Async[F]) =
    this(publicKeyProvider, epochTimeProvider, false)(F)

  /** check if mauth v2 only authenticate is enabled or not
    *
    * @return True or false identifying if v2 only authenticate is enabled or not.
    */
  override val isV2OnlyAuthenticate: Boolean = v2OnlyAuthenticate

  /** Performs the validation of an incoming HTTP request.
    *
    * The validation process consists of recreating the mAuth hashed signature from the request data
    * and comparing it to the decrypted hash signature from the mAuth header.
    *
    * @param mAuthRequest Data from the incoming HTTP request necessary to perform the validation.
    * @return True or false indicating if the request is valid or not with respect to mAuth.
    */
  override def authenticate(mAuthRequest: MAuthRequest)(implicit requestValidationTimeout: Duration): F[Boolean] = {
    if (!validateTime(mAuthRequest.getRequestTime)(requestValidationTimeout)) {
      logger.error(message(requestValidationTimeout))
      F.raiseError(new MAuthValidationException(message(requestValidationTimeout)))
    } else if (!validateMauthVersion(mAuthRequest, v2OnlyAuthenticate)) {

      logger.error(message)
      F.raiseError(new MAuthValidationException(message))
    } else {
      getPublicKeyF(mAuthRequest)
    }
  }

  def getPublicKeyF(mAuthRequest: MAuthRequest): F[Boolean] = {
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
            val v2IsValidated = validateSignatureV2(mAuthRequest, clientPublicKey)
            if (isV2OnlyAuthenticate)
              v2IsValidated
            else if (v2IsValidated)
              v2IsValidated
            else
              fallbackValidateSignatureV1(mAuthRequest, clientPublicKey)
        }
    }
  }

}
