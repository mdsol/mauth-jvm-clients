package com.mdsol.mauth.scaladsl.utils

import java.security.PublicKey
import java.util.UUID

trait ClientPublicKeyProvider[F[_]] {

  /**
   * Returns the associated public key for a given application UUID.
   *
   * @param appUUID
   *   UUID of the application for which we want to retrieve its public key.
   * @return
   *   An effectful value `F[Option[[[java.security.PublicKey]]]]` representing the public key registered in MAuth
   *   for the application with the given `appUUID`, if one exists.
   */
  def getPublicKey(appUUID: UUID): F[Option[PublicKey]]
}
