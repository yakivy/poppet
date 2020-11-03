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
    def materialize(): S = processor.process(input => for {
        request <- qcoder(input)
        response <- client(request)
        output <- scoder(response)
    } yield output)
}

object Consumer {
    def apply[I, F[_]] = new Builder[I, F]

    class Builder[I, F[_]] {
        def apply[S](
            client: Client[I, F])(processor: ConsumerProcessor[I, F, S])(
            implicit FM: Monad[F],
            qcoder: Coder[Request[I], F[I]],
            scoder: Coder[I, F[Response[I]]],
        ): Consumer[I, F, S] = new Consumer(client, processor)
    }
}