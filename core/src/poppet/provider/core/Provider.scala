package poppet.provider.core

import cats.data.OptionT
import cats.implicits._
import cats.Monad
import poppet.core.Request
import poppet.core.Response
import poppet.provider.all._
import poppet.provider.core.Provider._

/**
 * @tparam F service data kind, for example Future
 * @tparam I intermediate data type, for example Json
 */
class Provider[F[_]: Monad, I](
    fh: FailureHandler[F],
    processors: List[MethodProcessor[F, I]]
) extends Server[F, I] {

    private val indexedProcessors: Map[String, Map[String, Map[String, Map[String, I] => F[I]]]] =
        processors.groupBy(_.service).mapValues(
            _.groupBy(_.name).mapValues(
                _.map(m => m.arguments.sorted.mkString(",") -> m.f).toMap
            ).toMap
        ).toMap

    private def processorNotFoundFailure(processor: String, in: String): Failure = new Failure(
        s"Requested processor $processor is not in $in. Make sure that desired service is provided and up to date."
    )

    def apply(request: Request[I]): F[Response[I]] = for {
        serviceProcessors <- OptionT.fromOption[F](indexedProcessors.get(request.service))
            .getOrElseF(fh(processorNotFoundFailure(
                request.service,
                s"[${indexedProcessors.keySet.mkString(",")}]"
            )))
        methodProcessors <- OptionT.fromOption[F](serviceProcessors.get(request.method))
            .getOrElseF(fh(processorNotFoundFailure(
                s"${request.service}.${request.method}",
                s"${request.service}.[${serviceProcessors.keySet.mkString(",")}]"
            )))
        processor <- OptionT.fromOption[F](methodProcessors.get(request.arguments.keys.toList.sorted.mkString(",")))
            .getOrElseF(fh(processorNotFoundFailure(
                s"${request.service}.${request.method}(${request.arguments.keys.toList.sorted.mkString(",")})",
                s"${request.service}.${request.method}[${methodProcessors.keySet.map(p => s"($p)").mkString(",")}]"
            )))
        value <- processor(request.arguments)
    } yield Response(value)

    def service[S](s: S)(implicit processor: ProviderProcessor[F, I, S]) =
        new Provider[F, I](fh, processors ::: processor(s, fh))
}

object Provider {

    def apply[F[_]: Monad, I](
        fh: FailureHandler[F] = FailureHandler.throwing[F]
    ): Builder[F, I] = new Builder[F, I](fh)

    class Builder[F[_]: Monad, I](fh: FailureHandler[F]) {
        def service[S](s: S)(implicit processor: ProviderProcessor[F, I, S]): Provider[F, I] =
            new Provider[F, I](fh, Nil).service(s)
    }

}
