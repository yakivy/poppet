package poppet.coder

import poppet.dto.Request
import poppet.dto.Response

trait ExchangeCoder[A, I, F[_]] {
    def request: Coder[A, F[Request[I]]]
    def response: Coder[Response[I], F[A]]
}
