package poppet.coder

import cats.Id
import poppet.coder.CoderFixture.A
import poppet.dto.Request
import poppet.dto.Response

trait CoderFixture {
    val stringExample = "a"
    val intExample = 1
    val caseClassExample = A(stringExample, intExample)

    def assertExchangeCoder[A, I](coder: ExchangeCoder[A, I, Id])(implicit sc: Coder[String, I]): Unit = {
        val request = Request[I]("A", "a", Map("b" -> sc("c")))
        val response = Response[I](sc("c"))
        assert(coder.drequest(coder.erequest(request)) == request)
        assert(coder.dresponse(coder.eresponse(response)) == response)
    }

    def assertCustomCoder[A, I](
        coder: ExchangeCoder[_, I, Id])(value: A
    )(implicit fc: Coder[A, I], bc: Coder[I, A]): Unit = {
        assert(bc(fc(value)) == value)
    }
}

object CoderFixture {
    case class A(a: String, b: Int)
}
