package poppet.core

trait FailureHandler[F[_]] {
    def apply[A](f: Failure): F[A]
}

object FailureHandler {
    def throwing[F[_]]: FailureHandler[F] = new FailureHandler[F] {
        override def apply[A](f: Failure): F[A] = throw f
    }
}
