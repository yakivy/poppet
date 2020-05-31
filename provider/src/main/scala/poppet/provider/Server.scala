package poppet.provider

import poppet.coder.ExchangeCoder
import poppet.dto.Request
import poppet.dto.Response

trait Server[A, F[_], M] {
    def materialize[I](coder: ExchangeCoder[A, I, F])(f: Request[I] => F[Response[I]]): M
}
