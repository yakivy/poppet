package poppet.example.http4s

import _root_.poppet._
import _root_.poppet.example.http4s.model.SR
import cats.data.EitherT

package object poppet {
    val SRFailureHandler = new FailureHandler[SR] {
        override def apply[A](f: Failure): SR[A] = EitherT.leftT(f.getMessage)
    }
}
