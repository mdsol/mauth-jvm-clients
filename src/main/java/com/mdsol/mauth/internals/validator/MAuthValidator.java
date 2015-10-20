package com.mdsol.mauth.internals.validator;

import com.mdsol.mauth.domain.MAuthRequest;
import com.mdsol.mauth.exceptions.MAuthValidationException;

public interface MAuthValidator {

  /**
   * Performs the validation of an incoming HTTP request.
   * <p/>
   * The validation process consists of recreating the mAuth hashed signature from the request data
   * and comparing it to the decrypted hash signature from the mAuth header.
   * 
   * @param mAuthRequest Data from the incoming HTTP request necessary to perform the validation.
   * @return True or false indicating if the request is valid or not with respect to mAuth.
   * @throws MAuthValidationException
   */
  boolean validate(MAuthRequest mAuthRequest);

}
