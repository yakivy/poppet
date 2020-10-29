package poppet.provider.core

import cats.Monad
import cats.data.NonEmptyList
import cats.implicits._
import poppet._
import poppet.all.ExchangeCoder
import poppet.all.Request
import poppet.all.Response
import poppet.provider._

/**
 * @tparam I - intermediate data type, for example Json
 * @tparam F - service data kind, for example Future[_]
 */
class Provider[I, F[_] : Monad](
    processors: NonEmptyList[ProviderProcessor[I, F]])(
    implicit iqcoder: ExchangeCoder[Array[Byte], F[Request[I]]],
    bscoder: ExchangeCoder[Response[I], F[Array[Byte]]],
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
                .getOrElse(throw new Error("Can't find processor"))
        )
        value <- processor.f(request.arguments)
    } yield Response(value)

    def materialize(): Server[F] = irequest => for {
        brequest <- iqcoder(irequest)
        bresponse <- execute(brequest)
        iresponse <- bscoder(bresponse)
    } yield iresponse
}

object Provider {
    def apply[I, F[_]] = new Builder[I, F]()

    class Builder[I, F[_]]() {
        def apply(
            processor: ProviderProcessor[I, F], rest: ProviderProcessor[I, F]*)(
            implicit FM: Monad[F],
            iqcoder: ExchangeCoder[Array[Byte], F[Request[I]]],
            bscoder: ExchangeCoder[Response[I], F[Array[Byte]]],
        ): Provider[I, F] = new Provider(NonEmptyList(processor, rest.toList))
    }
}