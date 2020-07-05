package poppet.consumer

import cats.Monad
import cats.implicits._
import poppet.Decorator
import poppet.coder.ExchangeCoder

/**
 * @tparam A - client data type, for example Array[Byte]
 * @tparam I - intermediate data type, for example Json
 * @tparam F - client data kind, for example Future[_]
 * @tparam RQ - client request type
 * @tparam RS - client response type
 * @tparam S - service type, for example HelloService
 */
class Consumer[A, I, F[_] : Monad, RQ, RS, S](
    client: Client[A, F, RQ, RS], decorators: List[Decorator[RQ, RS, F]],
    coder: ExchangeCoder[A, I, F],
    processor: ConsumerProcessor[I, F, S]
) {
    def materialize(): S = processor.f(dequest => for {
        erequest <- coder.erequest(dequest)
        irequest <- client.buildRequest(erequest)
        iresponse <- decorators.foldLeft(client.execute _)((c, d) => d(c))(irequest)
        bresponse <- client.buildResponse(iresponse)
        dresponse <- coder.dresponse(bresponse)
    } yield dresponse)
}

object Consumer {
    def apply[A, I, F[_], RQ, RS, S](
        client: Client[A, F, RQ, RS],
        decorators: List[Decorator[RQ, RS, F]] = Nil
    ): PartialConsumerApply[A, F, RQ, RS, S] = new PartialConsumerApply(
        client, decorators
    )

    class PartialConsumerApply[A, F[_], RQ, RS, S](
        client: Client[A, F, RQ, RS], decorators: List[Decorator[RQ, RS, F]],
    ) {
        def apply[I](
            coder: ExchangeCoder[A, I, F])(processor: ConsumerProcessor[I, F, S]
        )(implicit F: Monad[F]): Consumer[A, I, F, RQ, RS, S] = new Consumer(
            client, decorators, coder, processor
        )
    }
}