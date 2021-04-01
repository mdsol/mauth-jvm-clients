package com.mdsol.mauth

import sttp.client3.{Request, Response}

trait SttpMAuthRequestSender[F[_]] {

  /** Takes an unsigned sttp request, populate the request headers with MAuth signatures and then sends it */
  def send[T](request: Request[T, Any]): F[Response[T]]
}
