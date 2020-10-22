package poppet.consumer

trait ConsumerDsl {
    type Consumer[I, F[_], S] = core.Consumer[I, F, S]
    type ConsumerProcessor[I, F[_], S] = core.ConsumerProcessor[I, F, S]
    type Client[F[_]] = Array[Byte] => F[Array[Byte]]

    val Consumer = core.Consumer
    val ConsumerProcessor = core.ConsumerProcessor
}
