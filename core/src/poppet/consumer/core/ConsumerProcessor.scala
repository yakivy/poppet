package poppet.consumer.core

import poppet.consumer.all._
import poppet.core.Request
import poppet.core.Response

trait ConsumerProcessor[F[_], I, S] {
    def apply(client: Request[I] => F[Response[I]], fh: FailureHandler[F]): S
}

object ConsumerProcessor extends ConsumerProcessorObjectBinCompat
