package poppet

import poppet.consumer.spring

trait SpringConsumerDsl extends ConsumerDsl {
    type SpringClient = spring.SpringClient

    val SpringClient = spring.SpringClient
}
