package poppet.example.spring.service

import org.springframework.http.RequestEntity
import org.springframework.http.ResponseEntity
import poppet.coder.jackson.all._
import poppet.provider.spring.all._

object ProviderGenerator {
    def apply(
        userService: UserService
    ): RequestEntity[Array[Byte]] => ResponseEntity[Array[Byte]] = {
        Provider(
            SpringServer(),
            JacksonCoder())(
            ProviderProcessor(userService).generate()
        ).materialize()
    }
}
