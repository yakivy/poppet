package poppet.consumer.core

import cats.Monad
import cats.implicits._
import poppet.consumer.all._
import poppet.core.Request
import poppet.core.Response

/**
 * @tparam I - intermediate data type, for example Json
 * @tparam F - client data kind, for example Future[_]
 * @tparam S - service type, for example HelloService
 */
class Consumer[I, F[_] : Monad, S](
    client: Client[I, F],
    processor: ConsumerProcessor[I, F, S])(
    implicit qcoder: Coder[Request[I], F[I]],
    scoder: Coder[I, F[Response[I]]],
) {
    def service: S = processor(input => for {
        request <- qcoder(input)
        response <- client(request)
        output <- scoder(response)
    } yield output)
}

object Consumer {
    def apply[I, F[_]](
        client: Client[I, F])(
        implicit FM: Monad[F],
        qcoder: Coder[Request[I], F[I]],
        scoder: Coder[I, F[Response[I]]]
    ) = new Builder[I, F](client)

    class Builder[I, F[_]](
        client: Client[I, F])(
        implicit FM: Monad[F],
        qcoder: Coder[Request[I], F[I]],
        scoder: Coder[I, F[Response[I]]]
    ) {
        def service[S](implicit processor: ConsumerProcessor[I, F, S]): S =
            new Consumer(client, processor).service
    }
}