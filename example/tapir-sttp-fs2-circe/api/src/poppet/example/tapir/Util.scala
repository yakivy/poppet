package poppet.example.tapir

import cats.effect.Concurrent
import fs2._

object Util {

    def uncons[F[_]: Concurrent, A](stream: Stream[F, A]): F[Option[(A, Stream[F, A])]] = stream.pull.uncons1.flatMap {
        case Some((h, t)) => Pull.extendScopeTo(t).flatMap(et => Pull.output1(h -> et))
        case None => Pull.done
    }.stream.compile.last

}
