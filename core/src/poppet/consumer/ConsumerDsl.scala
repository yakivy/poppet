package poppet.consumer

trait ConsumerDsl {
    type Consumer[F[_], I, S] = core.Consumer[F, I, S]
    type Transport[F[_], I] = Request[I] => F[Response[I]]

    val Consumer = core.Consumer
}
