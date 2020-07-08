package poppet.coder.jackson

import cats.Id
import org.scalatest.FreeSpec
import poppet.coder.CoderFixture
import poppet.coder.jackson.all._

class JacksonCoderSpec extends FreeSpec with CoderFixture {
    "Jackson coder should parse" - {
        val coder = JacksonCoder[Id]()
        "request and response data structures" in {
            assertExchangeCoder(coder)
        }
        "custom data structures" in {
            assertCustomCoder(coder)(())
            assertCustomCoder(coder)(intExample)
            assertCustomCoder(coder)(stringExample)
            assertCustomCoder(coder)(caseClassExample)
        }
    }
}
