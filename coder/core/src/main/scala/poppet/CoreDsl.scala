package poppet

trait CoreDsl {
    type ErrorHandler[A] = core.ErrorHandler[A]
    type ModelCoder[A, B] = core.ModelCoder[A, B]
    type ExchangeCoder[A, B] = core.ExchangeCoder[A, B]
    type Error = core.Error
    type Request[I] = core.Request[I]
    type Response[I] = core.Response[I]

    val Request = core.Request
    val Response = core.Response
}
