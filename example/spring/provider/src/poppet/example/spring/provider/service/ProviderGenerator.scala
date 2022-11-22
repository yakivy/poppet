package poppet.example.spring.provider.service

import cats.Id
import com.fasterxml.jackson.databind.JsonNode
import poppet.example.spring.service.UserService
import poppet.codec.jackson.all._
import poppet.provider.all._

object ProviderGenerator {
    def apply(userService: UserService): JsonNode => JsonNode =
        Provider[Id, JsonNode]().service(userService)
}
