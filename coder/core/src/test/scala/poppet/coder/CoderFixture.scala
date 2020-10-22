package poppet.coder

import cats.Id
import poppet.coder.CoderFixture.A
import poppet._

trait CoderFixture {
    val stringExample = "a"
    val intExample = 1
    val caseClassExample = A(stringExample, intExample)

    def assertExchangeCoder[I](coder: ExchangeCoder[I, Id])(implicit sc: Coder[String, I]): Unit = {
        val request = Request[I]("A", "a", Map("b" -> sc("c")))
        val response = Response[I](sc("c"))
        assert(coder.irequest(coder.brequest(request)) == request)
        assert(coder.iresponse(coder.bresponse(response)) == response)
    }

    def assertCustomCoder[I, A](
        coder: ExchangeCoder[I, Id])(value: A)(
        implicit fc: Coder[A, I], bc: Coder[I, A]
    ): Unit = {
        assert(bc(fc(value)) == value)
    }
}

object CoderFixture {
    case class A(a: String, b: Int)
}
