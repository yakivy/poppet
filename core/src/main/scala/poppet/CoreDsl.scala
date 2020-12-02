package poppet

import poppet.core.Request
import poppet.core.Response

trait CoreDsl {
    type FailureHandler[A] = core.FailureHandler[A]
    type Coder[A, B] = core.Coder[A, B]
    type Failure = core.Failure
    type Peek[I, F[_]] = (Request[I] => F[Response[I]]) => (Request[I] => F[Response[I]])
}
