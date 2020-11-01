package poppet.core

trait FunctionWithoutImplicitConversion[A, B] {
    def apply(a: A): B
}

trait FailureHandler[A] extends FunctionWithoutImplicitConversion[Failure, A]

trait ModelCoder[A, B] extends FunctionWithoutImplicitConversion[A, B]

trait ExchangeCoder[A, B] extends FunctionWithoutImplicitConversion[A, B]