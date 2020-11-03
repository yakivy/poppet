package poppet.core

trait FunctionWithoutImplicitConversion[A, B] {
    def apply(a: A): B
}

trait FailureHandler[A] extends FunctionWithoutImplicitConversion[Failure, A]

trait Coder[A, B] extends FunctionWithoutImplicitConversion[A, B]