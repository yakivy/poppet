package poppet

trait CoreDsl {
    type FailureHandler[A] = core.FailureHandler[A]
    type ModelCoder[A, B] = core.ModelCoder[A, B]
    type ExchangeCoder[A, B] = core.ExchangeCoder[A, B]
    type Failure = core.Failure
    type Request[I] = core.Request[I]
    type Response[I] = core.Response[I]

    val Request = core.Request
    val Response = core.Response
}
