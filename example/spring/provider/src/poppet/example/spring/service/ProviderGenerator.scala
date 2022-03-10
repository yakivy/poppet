package poppet.example.spring.service

import cats.Id
import com.fasterxml.jackson.databind.JsonNode
import poppet.codec.jackson.all._
import poppet.provider.all._

object ProviderGenerator {
    def apply(userService: UserService): JsonNode => JsonNode =
        Provider[Id, JsonNode]().service(userService)
}
