package poppet.consumer

import poppet.CoreDsl

trait ConsumerDsl extends CoreDsl {
    type Consumer[I, F[_], S] = core.Consumer[I, F, S]
    type ConsumerProcessor[I, F[_], S] = core.ConsumerProcessor[I, F, S]
    type Client[I, F[_]] = I => F[I]

    val Consumer = core.Consumer
    val ConsumerProcessor = core.ConsumerProcessor
}
