package poppet

trait CoreDsl {
    type Codec[A, B] = core.Codec[A, B]
    type CodecK[F[_], G[_]] = core.CodecK[F, G]
    type Failure = core.Failure
    type CodecFailure[I] = core.CodecFailure[I]
    type FailureHandler[F[_]] = core.FailureHandler[F]
    type Request[I] = core.Request[I]
    type Response[I] = core.Response[I]

    val FailureHandler = core.FailureHandler
    val Request = core.Request
    val Response = core.Response
}
