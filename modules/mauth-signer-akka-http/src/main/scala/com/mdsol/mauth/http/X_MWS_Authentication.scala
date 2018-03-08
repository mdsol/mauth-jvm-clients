package com.mdsol.mauth.http

import akka.http.scaladsl.model.headers.{ModeledCustomHeader, ModeledCustomHeaderCompanion}
import com.mdsol.mauth.MAuthRequest

import scala.util.Try

final class X_MWS_Authentication(token: String) extends ModeledCustomHeader[X_MWS_Authentication] {
  override def renderInRequests: Boolean = true
  override def renderInResponses: Boolean = true
  override val companion: X_MWS_Authentication.type = X_MWS_Authentication
  override def value: String = token
}
object X_MWS_Authentication extends ModeledCustomHeaderCompanion[X_MWS_Authentication] {
  override val name: String = MAuthRequest.X_MWS_AUTHENTICATION_HEADER_NAME
  override def parse(value: String): Try[X_MWS_Authentication] = Try(new X_MWS_Authentication(value))
}
