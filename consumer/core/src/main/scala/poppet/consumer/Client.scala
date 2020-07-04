package poppet.consumer

import poppet.coder.ExchangeCoder
import poppet.dto.Request
import poppet.dto.Response

trait Client[A, F[_]] {
    def materialize[I](coder: ExchangeCoder[A, I, F]): Request[I] => F[Response[I]]
}
