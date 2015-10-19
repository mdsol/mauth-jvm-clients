package com.mdsol.mauth.services;

import java.security.PublicKey;
import java.util.UUID;

public interface MAuthClient {

  PublicKey getPublicKey(UUID appUUID);

}
