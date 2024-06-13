package poppet.example.http4s

import cats.data.EitherT
import cats.effect.IO

package object model {
    /**
     * Service response type that based on IO effect and EitherT monad transformer
     * where left is a string error and right is an actual result
     */
    type SR[A] = EitherT[IO, String, A]
}
