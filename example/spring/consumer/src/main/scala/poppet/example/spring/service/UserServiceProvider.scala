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

class UserServiceProvider(restTemplate: RestTemplate)(url: String, secret: String) {
    private def client(
        restTemplate: RestTemplate)(url: String, authSecret: String
    ): Client[JsonNode, Id] = request => {
        val headers = new HttpHeaders()
        headers.add(HttpHeaders.AUTHORIZATION, authSecret)
        restTemplate.exchange(
            new RequestEntity(request, headers, HttpMethod.POST, URI.create(url)), classOf[JsonNode]
        ).getBody
    }

    def get: UserService = Consumer[JsonNode, Id](client(restTemplate)(url, secret)).service[UserService]
}
