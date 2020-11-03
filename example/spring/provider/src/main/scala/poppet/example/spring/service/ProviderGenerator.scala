package poppet.example.spring.service

import cats.Id
import com.fasterxml.jackson.databind.JsonNode
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.RequestEntity
import org.springframework.http.ResponseEntity
import poppet.coder.jackson.all._
import poppet.provider.all._

object ProviderGenerator {
    private def authDecorator(
        authSecret: String
    ): RequestEntity[JsonNode] => RequestEntity[JsonNode] = request => {
        if (request.getHeaders.get(HttpHeaders.AUTHORIZATION).contains(authSecret)) request
        else throw new IllegalArgumentException("Wrong secret!")
    }

    def apply(
        userService: UserService, authSecret: String
    ): RequestEntity[JsonNode] => ResponseEntity[JsonNode] = {
        val server = Provider[JsonNode, Id].apply(
            ProviderProcessor(userService).generate()
        ).materialize()
        request => new ResponseEntity(server(authDecorator(authSecret)(request).getBody), HttpStatus.OK)
    }
}
