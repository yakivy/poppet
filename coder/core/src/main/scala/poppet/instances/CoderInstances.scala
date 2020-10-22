package poppet.instances

import cats.Applicative
import cats.FlatMap
import poppet.Coder

trait CoderInstances {
    implicit def idCoder[A]: Coder[A, A] = identity(_)

    implicit def coderToFCoder[A, B, F[_]](implicit coder: Coder[A, B], FA: Applicative[F]): Coder[A, F[B]] =
        a => FA.pure(coder(a))

    implicit def coderToFsCoder[A, B, F[_]](implicit coder: Coder[A, B], FF: FlatMap[F]): Coder[F[A], F[B]] =
        a => FF.fmap(a)(ar => coder(ar))
}
