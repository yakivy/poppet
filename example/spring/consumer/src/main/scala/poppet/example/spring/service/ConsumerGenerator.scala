package poppet.example.spring.service

import cats.Id
import com.fasterxml.jackson.databind.JsonNode
import java.net.URI
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.RequestEntity
import org.springframework.web.client.RestTemplate
import poppet.coder.jackson.all._
import poppet.consumer._

object ConsumerGenerator {
    private def client(
        restTemplate: RestTemplate)(url: String, authSecret: String
    ): Client[Id] = request => {
        val headers = new HttpHeaders()
        headers.add(HttpHeaders.AUTHORIZATION, authSecret)
        restTemplate.exchange(
            new RequestEntity(request, headers, HttpMethod.POST, URI.create(url)), classOf[Array[Byte]]
        ).getBody
    }

    def userService(
        restTemplate: RestTemplate)(url: String, authSecret: String
    ): UserService = Consumer[JsonNode, Id].apply(
        client(restTemplate)(url, authSecret))(
        ConsumerProcessor[UserService].generate()
    ).materialize()
}
