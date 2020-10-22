package poppet.consumer.core

import cats.Monad
import cats.implicits._
import poppet.all._
import poppet.consumer.all._

/**
 * @tparam I - intermediate data type, for example Json
 * @tparam F - client data kind, for example Future[_]
 * @tparam S - service type, for example HelloService
 */
class Consumer[I, F[_] : Monad, S](
    client: Client[F],
    processor: ConsumerProcessor[I, F, S])(
    implicit rCoder: ExchangeCoder[]
) {
    def materialize(): S = processor.process(irequest => for {
        brequest <- coder.brequest(irequest)
        bresponse <- client(brequest)
        iresponse <- coder.iresponse(bresponse)
    } yield iresponse)
}

object Consumer {
    def apply[I, F[_]] = new Builder[I, F]

    class Builder[I, F[_]] {
        def apply[S](
            client: Client[F])(processor: ConsumerProcessor[I, F, S])(
            implicit FM: Monad[F]
        ): Consumer[I, F, S] = new Consumer(client, coder, processor)
    }
}