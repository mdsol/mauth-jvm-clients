package com.mdsol.mauth

import sttp.client.{Request, Response}

trait SttpMAuthRequestSender[F[_]] {

  /** Takes an unsigned sttp request, populate the request headers with MAuth signatures and then sends it */
  def send[T](request: Request[T, Nothing]): F[Response[T]]
}
