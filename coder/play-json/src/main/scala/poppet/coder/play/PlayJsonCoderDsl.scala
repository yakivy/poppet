package poppet.coder.play

trait PlayJsonCoderDsl {
    type PlayJsonCoder[F[_]] = poppet.coder.play.PlayJsonCoder[F]

    val PlayJsonCoder = poppet.coder.play.PlayJsonCoder
}
