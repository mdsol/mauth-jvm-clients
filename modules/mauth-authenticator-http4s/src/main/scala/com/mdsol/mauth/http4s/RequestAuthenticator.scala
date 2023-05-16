package com.mdsol.mauth.http4s

import cats.{ApplicativeThrow, MonadThrow}
import cats.implicits._
import com.mdsol.mauth.{MAuthRequest, MAuthVersion}
import com.mdsol.mauth.exception.MAuthValidationException
import com.mdsol.mauth.http4s.RequestAuthenticator.{fallbackValidateSignatureV1, validateMauthVersion, validateSignatureV1, validateSignatureV2, validateTime}
import com.mdsol.mauth.scaladsl.Authenticator
import com.mdsol.mauth.scaladsl.utils.ClientPublicKeyProvider
import com.mdsol.mauth.util.{EpochTimeProvider, MAuthSignatureHelper}
import org.typelevel.log4cats.Logger

import java.nio.charset.StandardCharsets
import java.security.PublicKey
import java.util
import scala.concurrent.duration._

class RequestAuthenticator[F[_]: MonadThrow: Logger](
  val publicKeyProvider: ClientPublicKeyProvider[F],
  override val epochTimeProvider: EpochTimeProvider,
  v2OnlyAuthenticate: Boolean
) extends Authenticator[F] {

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
    validateTime[F](mAuthRequest.getRequestTime, epochTimeProvider)(requestValidationTimeout)
      .flatMap {
        case false =>
          val message = s"MAuth request validation failed because of timeout $requestValidationTimeout"
          Logger[F].error(message) *> ApplicativeThrow[F].raiseError(new MAuthValidationException(message))
        case true =>
          validateMauthVersion[F](mAuthRequest, v2OnlyAuthenticate).flatMap {
            case false =>
              val message = "The service requires mAuth v2 authentication headers."
              Logger[F].error(message) *> ApplicativeThrow[F].raiseError(new MAuthValidationException(message))
            case _ => getPublicKey(mAuthRequest)
          }
      }
  }

  private def getPublicKey(mAuthRequest: MAuthRequest): F[Boolean] = {
    publicKeyProvider.getPublicKey(mAuthRequest.getAppUUID).flatMap {
      case None =>
        Logger[F].error("Public Key couldn't be retrieved") *> false.pure[F]
      case Some(clientPublicKey) =>
        // Decrypt the signature with public key from requesting application.
        mAuthRequest.getMauthVersion match {
          case MAuthVersion.MWS =>
            Logger[F].warn("MAuth v1 client was used to authenticate this request which is deprecated") *>
              validateSignatureV1[F](mAuthRequest, clientPublicKey)
          case MAuthVersion.MWSV2 if isV2OnlyAuthenticate => validateSignatureV2[F](mAuthRequest, clientPublicKey)
          case MAuthVersion.MWSV2 =>
            validateSignatureV2[F](mAuthRequest, clientPublicKey).flatMap {
              case true  => true.pure[F]
              case false => fallbackValidateSignatureV1[F](mAuthRequest, clientPublicKey)
            }
        }
    }
  }

}

object RequestAuthenticator {
  def apply[F[_]: MonadThrow: Logger](publicKeyProvider: ClientPublicKeyProvider[F], epochTimeProvider: EpochTimeProvider) =
    new RequestAuthenticator(publicKeyProvider, epochTimeProvider, v2OnlyAuthenticate = false)

  // Check epoch time is not older than specified interval.
  private def validateTime[F[_]: ApplicativeThrow](requestTime: Long, epochTimeProvider: EpochTimeProvider)(requestValidationTimeout: Duration): F[Boolean] =
    ((epochTimeProvider.inSeconds - requestTime) < requestValidationTimeout.toSeconds).pure[F]

  // Check V2 header if only V2 is required
  private def validateMauthVersion[F[_]: ApplicativeThrow](mAuthRequest: MAuthRequest, v2OnlyAuthenticate: Boolean): F[Boolean] =
    (!v2OnlyAuthenticate || mAuthRequest.getMauthVersion == MAuthVersion.MWSV2).pure[F]

  // check signature for V1
  private def validateSignatureV1[F[_]: ApplicativeThrow: Logger](mAuthRequest: MAuthRequest, clientPublicKey: PublicKey): F[Boolean] = {
    logAuthenticationRequest(mAuthRequest)
    val decryptedSignature = MAuthSignatureHelper.decryptSignature(clientPublicKey, mAuthRequest.getRequestSignature)
    // Recreate the plain text signature, based on the incoming request parameters, and hash it.
    ApplicativeThrow[F]
      .catchNonFatal {
        val messageDigest_bytes = MAuthSignatureHelper.generateDigestedMessageV1(mAuthRequest).getBytes(StandardCharsets.UTF_8)
        util.Arrays.equals(messageDigest_bytes, decryptedSignature)
      }
      .recoverWith { case ex: Exception =>
        val message = "MAuth request validation failed for V1."
        Logger[F].error(ex)(message) *> ApplicativeThrow[F].raiseError(new MAuthValidationException(message, ex))
      }
  }

  // check signature for V2
  private def validateSignatureV2[F[_]: ApplicativeThrow: Logger](mAuthRequest: MAuthRequest, clientPublicKey: PublicKey): F[Boolean] = {
    logAuthenticationRequest(mAuthRequest)
    // Recreate the plain text signature, based on the incoming request parameters, and hash it.
    val unencryptedRequestString = MAuthSignatureHelper.generateStringToSignV2(mAuthRequest)

    // Compare the decrypted signature and the recreated signature hashes.
    ApplicativeThrow[F]
      .catchNonFatal(MAuthSignatureHelper.verifyRSA(unencryptedRequestString, mAuthRequest.getRequestSignature, clientPublicKey))
      .recoverWith { case ex: Exception =>
        val message = "MAuth request validation failed for V2."
        Logger[F].error(ex)(message) *> ApplicativeThrow[F].raiseError(new MAuthValidationException(message, ex))
      }

  }

  private def fallbackValidateSignatureV1[F[_]: ApplicativeThrow: Logger](mAuthRequest: MAuthRequest, clientPublicKey: PublicKey): F[Boolean] = {
    var isValidated = false.pure[F]
    if (mAuthRequest.getMessagePayload == null) {
      Logger[F].warn("V1 authentication fallback is not available because the full request body is not available in memory.")
    } else if (mAuthRequest.getXmwsSignature != null && mAuthRequest.getXmwsTime != null) {
      val mAuthRequestV1 = new MAuthRequest(
        mAuthRequest.getXmwsSignature,
        mAuthRequest.getMessagePayload,
        mAuthRequest.getHttpMethod,
        mAuthRequest.getXmwsTime,
        mAuthRequest.getResourcePath,
        mAuthRequest.getQueryParameters
      )
      isValidated = validateSignatureV1[F](mAuthRequestV1, clientPublicKey).map {
        case true =>
          Logger[F].warn("Completed successful authentication attempt after fallback to V1")
          true
        case _ => false
      }
    }
    isValidated
  }

  private def logAuthenticationRequest[F[_]: Logger](mAuthRequest: MAuthRequest): F[Unit] = {
    val msgFormat = "Mauth-client attempting to authenticate request from app with mauth app uuid %s using version %s.".format(
      mAuthRequest.getAppUUID,
      mAuthRequest.getMauthVersion.getValue
    )
    Logger[F].info(msgFormat)
  }
}
