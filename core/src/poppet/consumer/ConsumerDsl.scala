package poppet.consumer

trait ConsumerDsl {
    type Consumer[F[_], I, S] = core.Consumer[F, I, S]
    type Transport[F[_], I] = I => F[I]

    val Consumer = core.Consumer
}
