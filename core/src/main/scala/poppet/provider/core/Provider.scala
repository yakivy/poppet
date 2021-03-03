package poppet.provider.core

import cats.Applicative
import cats.Monad
import cats.implicits._
import poppet.core.Request
import poppet.core.Response
import poppet.provider.all._

/**
 * @param peek function that can decorate given request -> response function without changing the types.
 * It is mostly used to peek on parsed dtos, for example for logging.
 *
 * @tparam I intermediate data type, for example Json
 * @tparam F service data kind, for example Future[_]
 */
class Provider[I, F[_] : Monad](
    peek: Peek[I, F],
    processors: List[MethodProcessor[I, F]])(
    implicit qcoder: Coder[I, F[Request[I]]],
    scoder: Coder[Response[I], F[I]],
    fh: FailureHandler[F[Map[String, I] => F[I]]]
) extends Server[I, F] {
    private val indexedProcessors: Map[String, Map[String, Map[String, Map[String, I] => F[I]]]] =
        processors.groupBy(_.service).mapValues(
            _.groupBy(_.name).mapValues(
                _.map(m => m.arguments.sorted.mkString(",") -> m.f).toMap
            ).toMap
        ).toMap

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
        value <- processor(request.arguments)
    } yield Response(value)

    def apply(request: I): F[I] = for {
        input <- qcoder(request)
        output <- peek(execute)(input)
        response <- scoder(output)
    } yield response

    def service[S](s: S)(implicit processor: ProviderProcessor[I, F, S]) =
        new Provider[I, F](peek, processors ::: processor(s))
}

object Provider {
    def apply[I, F[_]](
        peek: Peek[I, F] = identity[Request[I] => F[Response[I]]](_))(
        implicit FM: Monad[F],
        qcoder: Coder[I, F[Request[I]]],
        scoder: Coder[Response[I], F[I]],
        fh: FailureHandler[F[Map[String, I] => F[I]]]
    ): Builder[I, F] = new Builder[I, F](peek)

    class Builder[I, F[_]](
        peek: Peek[I, F])(
        implicit FM: Monad[F],
        qcoder: Coder[I, F[Request[I]]],
        scoder: Coder[Response[I], F[I]],
        fh: FailureHandler[F[Map[String, I] => F[I]]]
    ) {
        def service[S](s: S)(implicit processor: ProviderProcessor[I, F, S]): Provider[I, F] =
            new Provider[I, F](peek, Nil).service(s)
    }
}