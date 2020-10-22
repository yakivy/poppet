package poppet

trait CoreDsl {
    type Coder[A, B] = core.Coder[A, B]
    type Error = core.Error
    type ErrorHandler[E] = Error => E
    type Request[I] = core.Request[I]
    type Response[I] = core.Response[I]
    type ExchangeCoder[A, B] = core.ExchangeCoder[A, B]

    val Error = core.Error
    val Request = core.Request
    val Response = core.Response
}
