package com.mdsol.mauth.utils;

import java.security.PublicKey;
import java.util.UUID;

public interface ClientPublicKeyProvider {

  /**
   * Returns the associated public key for a given application UUID.
   *
   * @param appUUID, UUID of the application for which we want to retrieve its public key.
   * @return {@link java.security.PublicKey} registered in MAuth for the application with given appUUID.
   */
  PublicKey getPublicKey(UUID appUUID);
}
