package poppet

import poppet.consumer.play

trait PlayConsumerDsl extends ConsumerDsl {
    type PlayClient = play.PlayClient

    val PlayClient = play.PlayClient
}
