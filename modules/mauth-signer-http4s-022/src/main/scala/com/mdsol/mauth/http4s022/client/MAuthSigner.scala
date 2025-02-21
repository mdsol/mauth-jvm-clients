package com.mdsol.mauth.http4s022.client

import cats.effect._
import cats.syntax.all._
import com.mdsol.mauth.RequestSigner
import com.mdsol.mauth.models.UnsignedRequest
import org.http4s.Request
import org.http4s.client.Client
import org.http4s.EntityDecoder.byteArrayDecoder
import java.net.URI

object MAuthSigner {
  def apply[F[_]: Concurrent](signer: RequestSigner)(client: Client[F]): Client[F] =
    Client { req =>
      for {
        req <- Resource.eval(req.as[Array[Byte]].flatMap { byteArray =>
                 val signedRequest = signer.signRequest(
                   UnsignedRequest(
                     req.method.name,
                     URI.create(req.uri.renderString),
                     byteArray,
                     req.headers.headers.view.map(h => h.name.toString -> h.value).toMap
                   )
                 )
                 Request(
                   method = req.method,
                   uri = req.uri,
                   headers = req.headers.put(signedRequest.mauthHeaders.toList),
                   body = req.body
                 ).pure[F]
               })
        res <- client.run(req)
      } yield res
    }
}
