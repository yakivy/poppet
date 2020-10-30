package poppet.coder.cats.effect.instances

import cats.Id
import cats.effect.IO
import poppet.all._

trait CatsEffectCoderInstances {
    implicit def ioErrorHandler[A, B](
        implicit eh: ErrorHandler[A, Id[B]]
    ): ErrorHandler[A, IO[B]] = a => IO.delay(eh(a))
}
