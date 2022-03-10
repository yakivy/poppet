package poppet

import poppet.core.Request
import poppet.core.Response

trait CoreDsl {
    type Codec[A, B] = core.Codec[A, B]
    type CodecK[F[_], G[_]] = core.CodecK[F, G]
    type Failure = core.Failure
    type CodecFailure[I] = core.CodecFailure[I]
    type FailureHandler[F[_]] = core.FailureHandler[F]
    type Peek[F[_], I] = (Request[I] => F[Response[I]]) => (Request[I] => F[Response[I]])

    val FailureHandler = core.FailureHandler
}
