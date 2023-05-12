package com.mdsol.mauth.scaladsl.utils

import java.security.PublicKey
import java.util.UUID


trait ClientPublicKeyProvider[F[_]] {

  /** Returns the associated public key for a given application UUID.
    *
    * @param appUUID , UUID of the application for which we want to retrieve its public key.
    * @return Future of { @link PublicKey} registered in MAuth for the application with given appUUID.
    */
  def getPublicKey(appUUID: UUID): F[Option[PublicKey]]
}
