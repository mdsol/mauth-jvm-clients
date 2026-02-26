package com.mdsol.mauth

import cats.effect.IO
import sttp.client4.{Backend, Request, Response}

import scala.concurrent.Future

class SttpAkkaMAuthRequestSender(
  signer: MAuthSttpSigner,
  sttpBackend: Backend[Future]
) extends SttpMAuthRequestSender[IO] {
  override def send[T](request: Request[T]): IO[Response[T]] =
    IO.fromFuture(
      IO(
        signer.signSttpRequest(request).send(sttpBackend)
      )
    )
}
