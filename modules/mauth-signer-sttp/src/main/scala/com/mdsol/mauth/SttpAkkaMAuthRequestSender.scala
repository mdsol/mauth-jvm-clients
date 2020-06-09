package com.mdsol.mauth

import akka.stream.scaladsl.Source
import akka.util.ByteString
import sttp.client.{Request, Response, SttpBackend}
import cats.effect.{ContextShift, IO}

import scala.concurrent.Future

class SttpAkkaMAuthRequestSender(
  signer: MAuthSttpSigner,
  sttpBackend: SttpBackend[Future, Source[ByteString, Any], Nothing],
  contextShift: ContextShift[IO]
) extends SttpMAuthRequestSender[IO] {
  override def send[T](request: Request[T, Nothing]): IO[Response[T]] =
    IO.fromFuture(
      IO(
        signer.signSttpRequest(request).send()(sttpBackend, implicitly)
      )
    )(contextShift)
}
