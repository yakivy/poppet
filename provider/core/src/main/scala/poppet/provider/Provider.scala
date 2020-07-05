package poppet.provider

import cats.Monad
import cats.data.NonEmptyList
import cats.implicits._
import poppet.Decorator
import poppet.coder.ExchangeCoder
import poppet.dto.Request
import poppet.dto.Response

/**
 * @tparam A - server data type, for example Array[Byte]
 * @tparam I - intermediate data type, for example Json
 * @tparam F - service data kind, for example Future[_]
 * @tparam RQ - server request type
 * @tparam RS - server response type
 * @tparam M - materialized data type, for example Action
 */
class Provider[+A, I, F[_] : Monad, RQ, RS, M](
    server: Server[A, F, RQ, RS, M], decorators: List[Decorator[RQ, RS, F]],
    coder: ExchangeCoder[A, I, F],
    processors: NonEmptyList[ProviderProcessor[I, F]]
) {
    private val indexedProcessors: Map[String, Map[String, Map[String, MethodProcessor[I, F]]]] =
        processors.toList.groupBy(_.service).mapValues(
            _.flatMap(_.methods).groupBy(_.name).mapValues(
                _.map(m => m.arguments.toList.sorted.mkString(",") -> m).toMap
            ).toMap
        ).toMap
    require(
        processors.toList.flatMap(_.methods).size ==
            indexedProcessors.values.flatMap(_.values).flatMap(_.values).size,
        "Please use unique parameter name lists for overloaded methods"
    )

    private def execute(request: Request[I]): F[Response[I]] = {
        indexedProcessors.get(request.service)
            .flatMap(_.get(request.method))
            .flatMap(_.get(request.arguments.keys.toList.sorted.mkString(",")))
            .getOrElse(throw new IllegalStateException("Can't find processor"))
            .f(request.arguments)
            .map(result => Response(result))
    }

    def materialize(): M = server.materialize(decorators.foldLeft[RQ => F[RS]](irequest => for {
        irequest <- server.buildRequest(irequest)
        drequest <- coder.drequest(irequest)
        response <- execute(drequest)
        eresponse <- coder.eresponse(response)
        iresponse <- server.buildResponse(eresponse)
    } yield iresponse)((c, d) => d(c)))
}

object Provider {
    def apply[A, I, F[_], RQ, RS, M](
        server: Server[A, F, RQ, RS, M],
        decorators: List[Decorator[RQ, RS, F]] = Nil
    ): PartialProviderApply[A, F, RQ, RS, M] = new PartialProviderApply(
        server, decorators
    )

    class PartialProviderApply[A, F[_], RQ, RS, M](
        server: Server[A, F, RQ, RS, M], decorators: List[Decorator[RQ, RS, F]],
    ) {
        def apply[I](
            coder: ExchangeCoder[A, I, F])(processor: ProviderProcessor[I, F], rest: ProviderProcessor[I, F]*
        )(implicit F: Monad[F]): Provider[A, I, F, RQ, RS, M] = new Provider(
            server, decorators, coder, NonEmptyList(processor, rest.toList)
        )
    }
}