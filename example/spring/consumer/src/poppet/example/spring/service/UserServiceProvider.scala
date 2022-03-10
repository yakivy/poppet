package poppet.example.spring.service

import cats.Id
import com.fasterxml.jackson.databind.JsonNode
import java.net.URI
import org.springframework.http.HttpMethod
import org.springframework.http.RequestEntity
import org.springframework.web.client.RestTemplate
import poppet.codec.jackson.all._
import poppet.consumer._

class UserServiceProvider(restTemplate: RestTemplate)(url: String) {
    private def client(restTemplate: RestTemplate)(url: String): Transport[Id, JsonNode] =
        request => restTemplate.exchange(
            new RequestEntity(request, HttpMethod.POST, URI.create(url)), classOf[JsonNode]
        ).getBody

    def get: UserService = Consumer[Id, JsonNode](client(restTemplate)(url)).service[UserService]
}
