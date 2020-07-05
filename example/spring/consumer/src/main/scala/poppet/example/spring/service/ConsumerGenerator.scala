package poppet.example.spring.service

import cats.Id
import org.springframework.http.HttpHeaders
import org.springframework.http.RequestEntity
import org.springframework.http.ResponseEntity
import org.springframework.web.client.RestTemplate
import poppet.Decorator
import poppet.coder.jackson.all._
import poppet.consumer.spring.all._

object ConsumerGenerator {
    private def authDecorator(
        authHeader: String, authSecret: String
    ) = new Decorator[RequestEntity[Array[Byte]], ResponseEntity[Array[Byte]], Id] {
        override def apply(
            chain: RequestEntity[Array[Byte]] => ResponseEntity[Array[Byte]]
        ): RequestEntity[Array[Byte]] => ResponseEntity[Array[Byte]] = ((rq: RequestEntity[Array[Byte]]) => {
            val headers = new HttpHeaders()
            headers.add(authHeader, authSecret)
            new RequestEntity[Array[Byte]](rq.getBody, headers, rq.getMethod, rq.getUrl, rq.getType)
        }).andThen(chain)
    }

    def userService(
        restTemplate: RestTemplate)(url: String, authHeader: String, authSecret: String
    ): UserService = Consumer(
        SpringClient(url)(restTemplate), List(authDecorator(authHeader, authSecret)))(
        JacksonCoder())(
        ConsumerProcessor[UserService].generate()
    ).materialize()
}
