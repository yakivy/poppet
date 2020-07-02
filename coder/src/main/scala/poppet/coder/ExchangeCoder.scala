package poppet.coder

import poppet.dto.Request
import poppet.dto.Response

trait ExchangeCoder[A, I, F[_]] {
    def drequest: Coder[A, F[Request[I]]]
    def erequest: Coder[Request[I], F[A]]
    def dresponse: Coder[A, F[Response[I]]]
    def eresponse: Coder[Response[I], F[A]]
}
