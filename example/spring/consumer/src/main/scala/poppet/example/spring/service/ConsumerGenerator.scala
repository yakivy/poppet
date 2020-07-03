package poppet.example.spring.service

import org.springframework.web.client.RestTemplate
import poppet.coder.jackson.all._
import poppet.consumer.spring.all._

object ConsumerGenerator {
    def userService(restTemplate: RestTemplate): UserService = {
        Consumer(
            SpringClient("http://localhost:9001/api/service")(restTemplate),
            JacksonCoder())(
            ConsumerProcessor[UserService].generate()
        ).materialize()
    }
}
