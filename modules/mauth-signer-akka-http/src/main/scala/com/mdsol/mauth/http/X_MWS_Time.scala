package com.mdsol.mauth.http

import akka.http.scaladsl.model.headers.{ModeledCustomHeader, ModeledCustomHeaderCompanion}
import com.mdsol.mauth.MAuthRequest

import scala.util.Try

final class X_MWS_Time(token: String) extends ModeledCustomHeader[X_MWS_Time] {
  override def renderInRequests: Boolean = true
  override def renderInResponses: Boolean = true
  override val companion: X_MWS_Time.type = X_MWS_Time
  override def value: String = token
}
object X_MWS_Time extends ModeledCustomHeaderCompanion[X_MWS_Time] {
  override val name: String = MAuthRequest.X_MWS_TIME_HEADER_NAME
  override def parse(value: String): Try[X_MWS_Time] = Try(new X_MWS_Time(value))
}
