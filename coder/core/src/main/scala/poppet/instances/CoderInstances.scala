package poppet.instances

import cats.Applicative
import cats.FlatMap
import cats.implicits._
import poppet.all._

trait CoderInstances {
    implicit def throwErrorHandler[A]: ErrorHandler[A] = throw _
    implicit def fErrorHandler[A, F[_] : Applicative](eh: ErrorHandler[A]): ErrorHandler[F[A]] =
        a => Applicative[F].pure(eh(a))
    implicit def fModelCoder[A, B, F[_] : FlatMap](implicit mc: ModelCoder[A, F[B]]): ModelCoder[F[A], F[B]] =
        _.flatMap(mc.apply)
}
