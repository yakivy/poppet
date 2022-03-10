package poppet.core

trait CodecK[F[_], G[_]] {
    def apply[A](a: F[A]): G[A]
}

object CodecK {
    implicit def codecKIdentityInstance[F[_]]: CodecK[F, F] = new CodecK[F, F] {
        override def apply[A](a: F[A]): F[A] = a
    }
}
