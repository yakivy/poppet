package poppet.coder.circe

trait CirceCoderDsl {
    type CirceCoder[F[_]] = poppet.coder.circe.CirceCoder[F]

    val CirceCoder = poppet.coder.circe.CirceCoder
}
