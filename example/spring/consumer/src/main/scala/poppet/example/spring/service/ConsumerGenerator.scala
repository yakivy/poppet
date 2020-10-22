package poppet.example.spring.service

import cats.Id
import java.net.URI
import org.springframework.http.HttpMethod
import org.springframework.http.RequestEntity
import org.springframework.web.client.RestTemplate
import poppet.coder.jackson.all._
import poppet.consumer._
import poppet.example.spring.model.Context

object ConsumerGenerator {
    private def client(restTemplate: RestTemplate)(url: String): Client[Id] = request => restTemplate.exchange(
        new RequestEntity(request, HttpMethod.POST, URI.create(url)), classOf[Array[Byte]]
    ).getBody

    def userService(
        restTemplate: RestTemplate)(url: String, authSecret: String
    ): UserService = {
        Consumer[Id](
            client(restTemplate)(url),
            JacksonCoder())(
            ConsumerProcessor[UserService].generate()
        ).materialize()
    }
}
