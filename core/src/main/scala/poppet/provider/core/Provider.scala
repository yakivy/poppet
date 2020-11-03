package poppet.provider.core

import cats.Applicative
import cats.Monad
import cats.data.NonEmptyList
import cats.implicits._
import poppet.provider.all._
import poppet.core.Request
import poppet.core.Response

/**
 * @tparam I - intermediate data type, for example Json
 * @tparam F - service data kind, for example Future[_]
 */
class Provider[I, F[_] : Monad](
    processors: NonEmptyList[ProviderProcessor[I, F]])(
    implicit qcoder: Coder[I, F[Request[I]]],
    scoder: Coder[Response[I], F[I]],
    fh: FailureHandler[F[MethodProcessor[I, F]]]
) {
    private val indexedProcessors: Map[String, Map[String, Map[String, MethodProcessor[I, F]]]] =
        processors.toList.groupBy(_.service).mapValues(
            _.flatMap(_.methods).groupBy(_.name).mapValues(
                _.map(m => m.arguments.toList.sorted.mkString(",") -> m).toMap
            ).toMap
        ).toMap
    require(
        processors.forall(_.methods.nonEmpty),
        "Some of the passed processors have no methods. Are you sure that you passed trait as generic parameter during processor generation?"
    )
    require(
        processors.toList.flatMap(_.methods).size ==
            indexedProcessors.values.flatMap(_.values).flatMap(_.values).size,
        "Please use unique parameter name lists for overloaded methods."
    )

    private def execute(request: Request[I]): F[Response[I]] = for {
        processor <- Monad[F].pure(
            indexedProcessors.get(request.service)
                .flatMap(_.get(request.method))
                .flatMap(_.get(request.arguments.keys.toList.sorted.mkString(",")))
        ).flatMap {
            case Some(value) => Applicative[F].pure(value)
            case None => fh(new Failure(
                "Can't find processor. Make sure that your service is provided and up to date."
            ))
        }
        value <- processor.f(request.arguments)
    } yield Response(value)

    def materialize(): Server[I, F] = request => for {
        input <- qcoder(request)
        output <- execute(input)
        response <- scoder(output)
    } yield response
}

object Provider {
    def apply[I, F[_]] = new Builder[I, F]()

    class Builder[I, F[_]]() {
        def apply(
            processor: ProviderProcessor[I, F], rest: ProviderProcessor[I, F]*)(
            implicit FM: Monad[F],
            qcoder: Coder[I, F[Request[I]]],
            scoder: Coder[Response[I], F[I]],
            fh: FailureHandler[F[MethodProcessor[I, F]]],
        ): Provider[I, F] = new Provider(NonEmptyList(processor, rest.toList))
    }
}