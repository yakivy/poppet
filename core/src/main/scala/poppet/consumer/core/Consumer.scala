package poppet.consumer.core

import cats.Monad
import cats.implicits._
import poppet.consumer.all._
import poppet.core.Request
import poppet.core.Response

/**
 * @param transport function that transferring data to the provider
 * @param peek function that can decorate given request -> response function without changing the types.
 * It is mostly used to peek on parsed dtos, for example for logging.
 *
 * @tparam I intermediate data type, for example Json
 * @tparam F consumer data kind, for example Future[_]
 * @tparam S service type, for example HelloService
 */
class Consumer[I, F[_] : Monad, S](
    transport: Transport[I, F],
    peek: Peek[I, F],
    processor: ConsumerProcessor[I, F, S])(
    implicit qcoder: Coder[Request[I], F[I]],
    scoder: Coder[I, F[Response[I]]],
) {
    def service: S = processor(peek(input => for {
        request <- qcoder(input)
        response <- transport(request)
        output <- scoder(response)
    } yield output))
}

object Consumer {
    def apply[I, F[_]](
        client: Transport[I, F],
        peek: Peek[I, F] = identity[Request[I] => F[Response[I]]](_))(
        implicit FM: Monad[F],
        qcoder: Coder[Request[I], F[I]],
        scoder: Coder[I, F[Response[I]]]
    ): Builder[I, F] = new Builder[I, F](client, peek)

    class Builder[I, F[_]](
        client: Transport[I, F],
        peek: Peek[I, F])(
        implicit FM: Monad[F],
        qcoder: Coder[Request[I], F[I]],
        scoder: Coder[I, F[Response[I]]]
    ) {
        def service[S](implicit processor: ConsumerProcessor[I, F, S]): S =
            new Consumer(client, peek, processor).service
    }
}