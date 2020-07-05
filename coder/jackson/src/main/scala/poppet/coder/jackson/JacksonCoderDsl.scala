package poppet.coder.jackson

trait JacksonCoderDsl {
    type JacksonCoder[F[_]] = poppet.coder.jackson.JacksonCoder[F]

    val JacksonCoder = poppet.coder.jackson.JacksonCoder
}
