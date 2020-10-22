package poppet.coder.circe

import cats.Id
import io.circe.generic.auto._
import org.scalatest.FreeSpec
import poppet.coder.CoderFixture
import poppet.coder.circe.all._

class CirceCoderSpec extends FreeSpec with CoderFixture {
    "Play coder should parse" - {
        def coder = CirceCoder[Id]()
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
