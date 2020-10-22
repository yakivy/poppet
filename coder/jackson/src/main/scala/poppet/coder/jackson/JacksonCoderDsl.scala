package poppet.coder.jackson

trait JacksonCoderDsl {
    type JacksonCoder[F[_]] = core.JacksonCoder[F]

    val JacksonCoder = core.JacksonCoder
}
