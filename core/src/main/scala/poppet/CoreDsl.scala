package poppet

trait CoreDsl {
    type FailureHandler[A] = core.FailureHandler[A]
    type Coder[A, B] = core.Coder[A, B]
    type Failure = core.Failure
}
