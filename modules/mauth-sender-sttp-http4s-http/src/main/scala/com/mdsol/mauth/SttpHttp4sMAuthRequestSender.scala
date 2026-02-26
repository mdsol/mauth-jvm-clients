package com.mdsol.mauth

import cats.effect.IO
import sttp.client4.{Backend, Request, Response}

class SttpHttp4sMAuthRequestSender(
  signer: MAuthSttpSigner,
  sttpBackend: Backend[IO]
) extends SttpMAuthRequestSender[IO] {
  override def send[T](request: Request[T]): IO[Response[T]] =
    signer.signSttpRequest(request).send(sttpBackend)
}
