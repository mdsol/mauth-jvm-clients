package com.mdsol.mauth.services;

import java.security.PublicKey;

public interface MAuthClient {

  PublicKey getPublicKey(String appId);

}
