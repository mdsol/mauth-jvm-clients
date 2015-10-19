package com.mdsol.mauth.services;

import com.mdsol.mauth.domain.MAuthRequest;

public interface MAuthValidator {

  /**
   * Performs the validation of the incoming HTTP request.
   * <p/>
   * The validation process consists of recreating the mAuth hashed signature from the request data
   * and comparing it to the decrypted hash signature from the mAuth header.
   * 
   * @param mAuthRequest Data from the incoming request necessary to perform the validation.
   * @return True or false indicating if the request is valid or not with respect to mAuth.
   */
  boolean validate(MAuthRequest mAuthRequest);

}
