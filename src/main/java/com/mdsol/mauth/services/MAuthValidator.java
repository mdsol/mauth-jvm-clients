package com.mdsol.mauth.services;

import com.mdsol.mauth.domain.MAuthRequest;

public interface MAuthValidator {

  boolean validate(MAuthRequest mAuthRequest);

}
