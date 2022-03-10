package poppet.core

trait Codec[A, B] {
    def apply(a: A): Either[CodecFailure[A], B]
}

object Codec {
    implicit def codecIdentityInstance[A]: Codec[A, A] = new Codec[A, A] {
        override def apply(a: A): Either[CodecFailure[A], A] = Right(a)
    }
}