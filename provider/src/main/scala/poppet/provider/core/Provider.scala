package poppet.provider.core

import cats.Monad
import cats.data.NonEmptyList
import cats.implicits._
import poppet._
import poppet.provider._

/**
 * @tparam I - intermediate data type, for example Json
 * @tparam F - service data kind, for example Future[_]
 */
class Provider[I, F[_] : Monad](
    coder: ExchangeCoder[I, F],
    processors: NonEmptyList[ProviderProcessor[I, F]],
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

    private def execute(request: Request[I]): F[Response[I]] = for {
        processor <- Monad[F].pure(
            indexedProcessors.get(request.service)
                .flatMap(_.get(request.method))
                .flatMap(_.get(request.arguments.keys.toList.sorted.mkString(",")))
                .getOrElse(throw Error("Can't find processor"))
        )
        value <- processor.f(request.arguments)
    } yield Response(value)

    def materialize(): Server[F] = irequest => for {
        brequest <- coder.irequest(irequest)
        bresponse <- execute(brequest)
        iresponse <- coder.bresponse(bresponse)
    } yield iresponse
}

object Provider {
    def apply[F[_]] = new Builder[F]()

    class Builder[F[_]]() {
        def apply[I](
            coder: ExchangeCoder[I, F])(processor: ProviderProcessor[I, F], rest: ProviderProcessor[I, F]*)(
            implicit FM: Monad[F]
        ): Provider[I, F] = new Provider(coder, NonEmptyList(processor, rest.toList))
    }
}