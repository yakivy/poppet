package poppet

trait PlayWsConsumerDsl extends ConsumerDsl {
    type PlayWsClient = poppet.consumer.play.PlayWsClient

    val PlayWsClient = poppet.consumer.play.PlayWsClient
}
