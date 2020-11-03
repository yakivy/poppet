package poppet.coder.circe.instances

import cats.Applicative
import io.circe.Decoder
import io.circe.Encoder
import io.circe.Json
import poppet.core._
import poppet.coder.instances.CoderInstances

trait CirceCoderInstances extends CoderInstances {
    implicit def decoderToCoder[A, F[_] : Applicative](
        implicit d: Decoder[A], fh: FailureHandler[F[A]]
    ): Coder[Json, F[A]] = a => d(a.hcursor) match {
        case Right(value) => Applicative[F].pure(value)
        case Left(value) => fh(new Failure(s"Decoding error: ${value.getMessage()}", value))
    }
    implicit def encoderToCoder[A, F[_] : Applicative](
        implicit e: Encoder[A]
    ): Coder[A, F[Json]] = a => Applicative[F].pure(e(a))
}
