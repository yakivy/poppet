package poppet.example.spring.service

import cats.Id
import com.fasterxml.jackson.databind.JsonNode
import poppet.coder.jackson.all._
import poppet.provider.all._

object ProviderGenerator {
    def apply(userService: UserService, authSecret: String): JsonNode => JsonNode =
        Provider[JsonNode, Id]().service(userService)
}
