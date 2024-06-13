package poppet.example.spring.consumer.service

import cats.Id
import com.fasterxml.jackson.core.`type`.TypeReference
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import java.net.URI
import org.springframework.http.HttpMethod
import org.springframework.http.RequestEntity
import org.springframework.web.client.RestTemplate
import poppet.codec.jackson.all._
import poppet.consumer._
import poppet.example.spring.service.UserService

class UserServiceProvider(restTemplate: RestTemplate)(url: String)(implicit objectMapper: ObjectMapper) {

    private def client(restTemplate: RestTemplate)(url: String): Transport[Id, JsonNode] = request => {
        objectMapper.readValue(
            objectMapper.treeAsTokens(restTemplate.exchange(
                new RequestEntity[JsonNode](
                    objectMapper.valueToTree[JsonNode](request),
                    HttpMethod.POST,
                    URI.create(url)
                ),
                classOf[JsonNode]
            ).getBody),
            new TypeReference[Response[JsonNode]]() {}
        )
    }

    def get: UserService = Consumer[Id, JsonNode](client(restTemplate)(url)).service[UserService]
}
