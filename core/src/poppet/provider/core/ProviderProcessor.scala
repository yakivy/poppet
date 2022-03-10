package poppet.provider.core

import poppet.core.FailureHandler

trait ProviderProcessor[F[_], I, S] {
    def apply(service: S, fh: FailureHandler[F]): List[MethodProcessor[F, I]]
}

class MethodProcessor[F[_], I](
    val service: String, val name: String, val arguments: List[String], val f: Map[String, I] => F[I]
)

object ProviderProcessor extends ProviderProcessorObjectBinCompat
