package poppet.coder.instances

import cats.Applicative
import cats.FlatMap
import cats.implicits._
import poppet.core._

trait CoderInstances {
    implicit def throwingFailureHandler[A]: FailureHandler[A] = throw _
    implicit def fFailureHandler[A, F[_] : Applicative](fh: FailureHandler[A]): FailureHandler[F[A]] =
        a => Applicative[F].pure(fh(a))
    implicit def fCoder[A, B, F[_] : FlatMap](implicit mc: Coder[A, F[B]]): Coder[F[A], F[B]] =
        _.flatMap(mc.apply)
}
