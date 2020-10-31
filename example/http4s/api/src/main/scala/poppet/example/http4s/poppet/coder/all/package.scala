package poppet.example.http4s.poppet.coder

import cats.data.EitherT
import poppet.all.ErrorHandler
import poppet.coder.circe.instances.CirceCoderInstances
import poppet.example.http4s.model.SR

package object all extends CirceCoderInstances {
    implicit def srParsingErrorHandler[A, E <: Throwable]: ErrorHandler[SR[A]] =
        a => EitherT.leftT(a.getMessage)
}