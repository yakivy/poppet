package poppet

trait ConsumerDsl {
    type Consumer[A, I, F[_], RQ, RS, S] = poppet.consumer.Consumer[A, I, F, RQ, RS, S]
    type ConsumerProcessor[I, F[_], S] = poppet.consumer.ConsumerProcessor[I, F, S]

    val Consumer = poppet.consumer.Consumer
    val ConsumerProcessor = poppet.consumer.ConsumerProcessor
}
