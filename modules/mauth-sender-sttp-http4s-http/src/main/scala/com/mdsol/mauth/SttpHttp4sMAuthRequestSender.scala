package com.mdsol.mauth

import cats.effect.IO
import sttp.client3.{Request, Response, SttpBackend}
import sttp.capabilities.fs2.Fs2Streams

class SttpHttp4sMAuthRequestSender(
  signer: MAuthSttpSigner,
  sttpBackend: SttpBackend[IO, Fs2Streams[IO]]
) extends SttpMAuthRequestSender[IO] {
  override def send[T](request: Request[T, Any]): IO[Response[T]] =
    sttpBackend.send(signer.signSttpRequest(request))
}
