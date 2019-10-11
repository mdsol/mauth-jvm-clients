package com.mdsol.mauth.scaladsl

import com.mdsol.mauth.MAuthRequest

import scala.concurrent.duration.Duration
import scala.concurrent.{ExecutionContext, Future}

trait Authenticator {

  /**
    * Performs the validation of an incoming HTTP request.
    *
    * The validation process consists of recreating the mAuth hashed signature from the request data
    * and comparing it to the decrypted hash signature from the mAuth header.
    *
    * @param mAuthRequest Data from the incoming HTTP request necessary to perform the validation.
    * @return True or false indicating if the request is valid or not with respect to mAuth.
    */
  def authenticate(mAuthRequest: MAuthRequest)(implicit ex: ExecutionContext, requestValidationTimeout: Duration): Future[Boolean]
}
