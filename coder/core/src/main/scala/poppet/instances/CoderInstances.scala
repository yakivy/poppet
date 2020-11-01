package poppet.instances

import cats.Applicative
import cats.FlatMap
import cats.implicits._
import poppet.all._

trait CoderInstances {
    implicit def throwingFailureHandler[A]: FailureHandler[A] = throw _
    implicit def fFailureHandler[A, F[_] : Applicative](fh: FailureHandler[A]): FailureHandler[F[A]] =
        a => Applicative[F].pure(fh(a))
    implicit def fModelCoder[A, B, F[_] : FlatMap](implicit mc: ModelCoder[A, F[B]]): ModelCoder[F[A], F[B]] =
        _.flatMap(mc.apply)
}
