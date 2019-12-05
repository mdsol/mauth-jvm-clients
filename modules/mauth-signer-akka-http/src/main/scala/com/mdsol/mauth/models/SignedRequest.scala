package com.mdsol.mauth.models

final case class SignedRequest(
  req: UnsignedRequest,
  mauthHeaders: Map[String, String]
)
