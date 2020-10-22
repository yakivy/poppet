package poppet.example.spring.service

import cats.Id
import org.springframework.http.HttpStatus
import org.springframework.http.RequestEntity
import org.springframework.http.ResponseEntity
import poppet.coder.jackson.all._
import poppet.provider.all._

object ProviderGenerator {
    private def authDecorator(
        authHeader: String, authSecret: String,
    ): RequestEntity[Array[Byte]] => RequestEntity[Array[Byte]] = request => {
        if (request.getHeaders.get(authHeader).contains(authSecret)) request
        else throw new IllegalArgumentException("Wrong secret!")
    }

    def apply(
        userService: UserService)(authHeader: String, authSecret: String
    ): RequestEntity[Array[Byte]] => ResponseEntity[Array[Byte]] = {
        Provider[Id](
            JacksonCoder())(
            ProviderProcessor(userService).generate()
        ).materialize()
            .compose((_: RequestEntity[Array[Byte]]).getBody)
            .compose(authDecorator(authHeader, authSecret))
            .andThen(new ResponseEntity(_, HttpStatus.OK))
    }
}
