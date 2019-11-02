package com.mdsol.mauth.http

import akka.http.scaladsl.model.headers.{ModeledCustomHeader, ModeledCustomHeaderCompanion}
import com.mdsol.mauth.MAuthRequest

import scala.util.Try

final class `X-MWS-Authentication`(token: String) extends ModeledCustomHeader[`X-MWS-Authentication`] {
  override def renderInRequests: Boolean = true

  override def renderInResponses: Boolean = true

  override val companion: `X-MWS-Authentication`.type = `X-MWS-Authentication`

  override def value: String = token
}

object `X-MWS-Authentication` extends ModeledCustomHeaderCompanion[`X-MWS-Authentication`] {
  override val name: String = MAuthRequest.X_MWS_AUTHENTICATION_HEADER_NAME

  override def parse(value: String): Try[`X-MWS-Authentication`] = Try(new `X-MWS-Authentication`(value))
}

final class `X-MWS-Time`(token: String) extends ModeledCustomHeader[`X-MWS-Time`] {
  override def renderInRequests: Boolean = true

  override def renderInResponses: Boolean = true

  override val companion: `X-MWS-Time`.type = `X-MWS-Time`

  override def value: String = token
}

object `X-MWS-Time` extends ModeledCustomHeaderCompanion[`X-MWS-Time`] {
  override val name: String = MAuthRequest.X_MWS_TIME_HEADER_NAME

  override def parse(value: String): Try[`X-MWS-Time`] = Try(new `X-MWS-Time`(value))
}

final class `MCC-Authentication`(token: String) extends ModeledCustomHeader[`MCC-Authentication`] {
  override def renderInRequests: Boolean = true

  override def renderInResponses: Boolean = true

  override val companion: `MCC-Authentication`.type = `MCC-Authentication`

  override def value: String = token
}

object `MCC-Authentication` extends ModeledCustomHeaderCompanion[`MCC-Authentication`] {
  override val name: String = MAuthRequest.MCC_AUTHENTICATION_HEADER_NAME

  override def parse(value: String): Try[`MCC-Authentication`] = Try(new `MCC-Authentication`(value))
}

final class `MCC-Time`(token: String) extends ModeledCustomHeader[`MCC-Time`] {
  override def renderInRequests: Boolean = true

  override def renderInResponses: Boolean = true

  override val companion: `MCC-Time`.type = `MCC-Time`

  override def value: String = token
}

object `MCC-Time` extends ModeledCustomHeaderCompanion[`MCC-Time`] {
  override val name: String = MAuthRequest.MCC_TIME_HEADER_NAME

  override def parse(value: String): Try[`MCC-Time`] = Try(new `MCC-Time`(value))
}
