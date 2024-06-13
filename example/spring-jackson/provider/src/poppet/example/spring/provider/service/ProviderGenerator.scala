package poppet.example.spring.provider.service

import cats.Id
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Service
import poppet.codec.jackson.all._
import poppet.example.spring.service.UserService
import poppet.provider.all._

@Service
class ProviderGenerator(userService: UserService)(implicit objectMapper: ObjectMapper) {
    private val provider = Provider[Id, JsonNode]().service(userService)

    def apply: Request[JsonNode] => Response[JsonNode] = request => provider.apply(request)
}
