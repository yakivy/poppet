package poppet.coder.circe

import io.circe.Json
import io.circe.generic.auto._
import org.scalatest.FreeSpec
import poppet.coder.CoderFixture
import poppet.coder.CoderFixture.A
import poppet.coder.circe.all._

class CirceCoderSpec extends FreeSpec with CoderFixture {
    "Play coder should parse" - {
        "request and response data structures" in {
            assertExchangeCoder[Json]
        }
        "custom data structures" in {
            assertCustomCoder[Json, Unit](())
            assertCustomCoder[Json, Int](intExample)
            assertCustomCoder[Json, String](stringExample)
            assertCustomCoder[Json, A](caseClassExample)
        }
    }
}
