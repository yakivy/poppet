package poppet.coder.instances

import cats.Functor
import poppet.coder.Coder
import scala.concurrent.Future

trait CoderInstances {
    implicit def idCoder[A]: Coder[A, A] = identity(_)

    implicit def coderToFutureCoder[A, B](implicit coder: Coder[A, B]): Coder[A, Future[B]] =
        a => Future.successful(coder(a))

    implicit def coderToFuturesCoder[A, B](
        implicit coder: Coder[A, B], F: Functor[Future]
    ): Coder[Future[A], Future[B]] = a => F.fmap(a)(ar => coder(ar))
}
