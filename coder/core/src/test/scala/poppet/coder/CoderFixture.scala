package poppet.coder

import cats.Id
import poppet.all._
import poppet.coder.CoderFixture.A

trait CoderFixture {
    val stringExample = "a"
    val intExample = 1
    val caseClassExample = A(stringExample, intExample)

    def assertExchangeCoder[I](
        implicit sc: ModelCoder[String, Id[I]],
        bqcoder: ExchangeCoder[Request[I], Id[Array[Byte]]],
        iscoder: ExchangeCoder[Array[Byte], Id[Response[I]]],
        iqcoder: ExchangeCoder[Array[Byte], Id[Request[I]]],
        bscoder: ExchangeCoder[Response[I], Id[Array[Byte]]],
    ): Unit = {
        val request = Request[I]("A", "a", Map("b" -> sc("c")))
        val response = Response[I](sc("c"))
        assert(iqcoder(bqcoder(request)) == request)
        assert(iscoder(bscoder(response)) == response)
    }

    def assertCustomCoder[I, A](value: A)(
        implicit fc: ModelCoder[A, Id[I]],
        bc: ModelCoder[I, Id[A]],
    ): Unit = {
        assert(bc(fc(value)) == value)
    }
}

object CoderFixture {
    case class A(a: String, b: Int)
}
