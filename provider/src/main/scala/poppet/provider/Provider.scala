package poppet.provider

import cats.Functor
import cats.implicits._
import poppet.coder.ExchangeCoder
import poppet.dto.Response

case class Provider[A, I, F[_] : Functor, M](
    server: Server[A, F, M], coder: ExchangeCoder[A, I, F])(
    processors: ProviderProcessor[I, F]
) {
    private val indexedProcessors: Map[String, Map[String, Map[String, MethodProcessor[I, F]]]] =
        List(processors).groupBy(_.service).mapValues(
            _.flatMap(_.methods).groupBy(_.name).mapValues(
                _.map(m => m.arguments.toList.sorted.mkString(",") -> m).toMap
            ).toMap
        ).toMap
    require(
        List(processors).flatMap(_.methods).size == indexedProcessors.values.flatMap(_.values).flatMap(_.values).size,
        "Please use unique parameter name lists for overloaded methods"
    )

    def materialize(): M = server.materialize(coder) { r =>
        indexedProcessors.get(r.service)
            .flatMap(_.get(r.method))
            .flatMap(_.get(r.arguments.keys.toList.sorted.mkString(",")))
            .getOrElse(throw new IllegalStateException("Can't find processor"))
            .f(r.arguments)
            .map(result => Response(result))
    }
}
