package poppet.consumer

import cats.Functor
import poppet.coder.ExchangeCoder

/**
 * @tparam A - client data type, for example Array[Byte]
 * @tparam I - intermediate data type, for example Json
 * @tparam F - client data kind, for example Future[_]
 * @tparam S - service type, for example HelloService
 */
class Consumer[A, I, F[_] : Functor, S](
    client: Client[A, F], coder: ExchangeCoder[A, I, F])(
    processor: ConsumerProcessor[I, F, S]
) {
    def materialize(): S = processor.f(client.materialize(coder))
}

object Consumer {
    def apply[A, I, F[_] : Functor, S](
        client: Client[A, F], coder: ExchangeCoder[A, I, F])(processor: ConsumerProcessor[I, F, S]
    ): Consumer[A, I, F, S] = new Consumer(client, coder)(processor)
}