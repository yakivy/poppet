package poppet.provider

import poppet.coder.BiCoder
import poppet.dto.Request
import poppet.dto.Response

trait Server[A, F[_], M] {
    def materialize[I](coder: BiCoder[A, I, F])(f: Request[I] => Response[I]): M
}
