package poppet.consumer.core

import cats.implicits._
import cats.Monad
import poppet.consumer.all._

/**
 * @param transport function that transfers data to the provider
 *
 * @tparam F consumer data kind, for example Future[_]
 * @tparam I intermediate data type, for example Json
 * @tparam S service type, for example HelloService
 */
class Consumer[F[_]: Monad, I, S](
    transport: Transport[F, I],
    fh: FailureHandler[F],
    processor: ConsumerProcessor[F, I, S]
) {
    def service: S = processor(transport, fh)
}

object Consumer {

    def apply[F[_], I](
        client: Transport[F, I],
        fh: FailureHandler[F] = FailureHandler.throwing[F]
    )(implicit
        F: Monad[F],
    ): Builder[F, I] = new Builder[F, I](client, fh)

    class Builder[F[_], I](
        client: Transport[F, I],
        fh: FailureHandler[F]
    )(implicit
        F: Monad[F],
    ) {
        def service[S](implicit processor: ConsumerProcessor[F, I, S]): S =
            new Consumer(client, fh, processor).service
    }

}
