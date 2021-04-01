package com.mdsol.mauth

import sttp.client3.{Request, Response, SttpBackend}
import cats.effect.{ContextShift, IO}

import scala.concurrent.Future

class SttpAkkaMAuthRequestSender(
  signer: MAuthSttpSigner,
  sttpBackend: SttpBackend[Future, Any],
  contextShift: ContextShift[IO]
) extends SttpMAuthRequestSender[IO] {
  override def send[T](request: Request[T, Any]): IO[Response[T]] =
    IO.fromFuture(
      IO(
        sttpBackend.send(signer.signSttpRequest(request))
      )
    )(contextShift)
}
