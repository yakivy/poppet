package poppet.example.spring.service

import cats.Id
import org.springframework.http.RequestEntity
import org.springframework.http.ResponseEntity
import poppet.Decorator
import poppet.coder.jackson.all._
import poppet.provider.spring.all._

object ProviderGenerator {
    private def authDecorator(
        authHeader: String, authSecret: String
    ) = new Decorator[RequestEntity[Array[Byte]], ResponseEntity[Array[Byte]], Id] {
        override def apply(
            chain: RequestEntity[Array[Byte]] => ResponseEntity[Array[Byte]]
        ): RequestEntity[Array[Byte]] => ResponseEntity[Array[Byte]] = ((rq: RequestEntity[Array[Byte]]) => {
            if (!rq.getHeaders.get(authHeader).contains(authSecret))
                throw new IllegalArgumentException("Wrong secret!")
            else rq
        }).andThen(chain)
    }

    def apply(
        userService: UserService)(authHeader: String, authSecret: String
    ): RequestEntity[Array[Byte]] => ResponseEntity[Array[Byte]] = Provider(
        SpringServer(), List(authDecorator(authHeader, authSecret)))(
        JacksonCoder())(
        ProviderProcessor(userService).generate()
    ).materialize()
}
