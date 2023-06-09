package poppet.consumer.core

import cats.Monad
import cats.implicits._
import poppet.consumer.all._
import poppet.core.Request
import poppet.core.Response

/**
 * @param transport function that transfers data to the provider
 * @param peek function that can decorate given request -> response function without changing the types.
 * It is mostly used to peek on parsed dtos, for example for logging.
 *
 * @tparam F consumer data kind, for example Future[_]
 * @tparam I intermediate data type, for example Json
 * @tparam S service type, for example HelloService
 */
class Consumer[F[_] : Monad, I, S](
    transport: Transport[F, I],
    peek: Peek[F, I],
    fh: FailureHandler[F],
    processor: ConsumerProcessor[F, I, S])(
    implicit qcodec: Codec[Request[I], I],
    scodec: Codec[I, Response[I]],
) {
    def service: S = processor(
        peek(input => for {
            request <- qcodec(input).fold(fh.apply, Monad[F].pure)
            response <- transport(request)
            output <- scodec(response).fold(fh.apply, Monad[F].pure)
        } yield output),
        fh,
    )
}

object Consumer {
    def apply[F[_], I](
        client: Transport[F, I],
        peek: Peek[F, I] = identity[Request[I] => F[Response[I]]](_),
        fh: FailureHandler[F] = FailureHandler.throwing[F])(
        implicit FM: Monad[F],
        qcodec: Codec[Request[I], I],
        scodec: Codec[I, Response[I]]
    ): Builder[F, I] = new Builder[F, I](client, peek, fh)

    class Builder[F[_], I](
        client: Transport[F, I],
        peek: Peek[F, I],
        fh: FailureHandler[F])(
        implicit FM: Monad[F],
        qcodec: Codec[Request[I], I],
        scodec: Codec[I, Response[I]]
    ) {
        def service[S](implicit processor: ConsumerProcessor[F, I, S]): S =
            new Consumer(client, peek, fh, processor).service
    }
}