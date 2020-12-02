package poppet.consumer

import poppet.CoreDsl

trait ConsumerDsl extends CoreDsl {
    type Consumer[I, F[_], S] = core.Consumer[I, F, S]
    type Transport[I, F[_]] = I => F[I]

    val Consumer = core.Consumer
}
