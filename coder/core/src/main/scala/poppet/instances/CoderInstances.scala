package poppet.instances

import cats.FlatMap
import cats.Id
import cats.implicits._
import poppet.all._
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

trait CoderInstances {
    implicit def futureErrorHandler[A, B](
        implicit eh: ErrorHandler[A, Id[B]], ec: ExecutionContext
    ): ErrorHandler[A, Future[B]] = a => Future.apply(eh(a))
    implicit def identityErrorHandler[A]: ErrorHandler[A, Id[A]] = identity(_)
    implicit def fModelCoder[A, B, F[_] : FlatMap](implicit mc: ModelCoder[A, F[B]]): ModelCoder[F[A], F[B]] =
        _.flatMap(mc)
}
