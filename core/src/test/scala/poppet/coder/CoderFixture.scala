package poppet.coder

import cats.Id
import poppet.core._
import poppet.coder.CoderFixture.A

trait CoderFixture {
    val stringExample = "a"
    val intExample = 1
    val caseClassExample = A(stringExample, intExample)

    def assertExchangeCoder[I](
        implicit sc: Coder[String, Id[I]],
        bqcoder: Coder[Request[I], Id[I]],
        iscoder: Coder[I, Id[Response[I]]],
        iqcoder: Coder[I, Id[Request[I]]],
        bscoder: Coder[Response[I], Id[I]],
    ): Unit = {
        val request = Request[I]("A", "a", Map("b" -> sc("c")))
        val response = Response[I](sc("c"))
        assert(iqcoder(bqcoder(request)) == request)
        assert(iscoder(bscoder(response)) == response)
    }

    def assertCustomCoder[I, A](value: A)(
        implicit fc: Coder[A, Id[I]],
        bc: Coder[I, Id[A]],
    ): Unit = {
        assert(bc(fc(value)) == value)
    }
}

object CoderFixture {
    case class A(a: String, b: Int)
}
