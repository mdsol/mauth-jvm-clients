package com.mdsol.mauth

import cats.effect.IO
import sttp.client3.{Request, Response, SttpBackend}

import scala.concurrent.Future

class SttpAkkaMAuthRequestSender(
  signer: MAuthSttpSigner,
  sttpBackend: SttpBackend[Future, Any]
) extends SttpMAuthRequestSender[IO] {
  override def send[T](request: Request[T, Any]): IO[Response[T]] =
    IO.fromFuture(
      IO(
        sttpBackend.send(signer.signSttpRequest(request))
      )
    )
}
