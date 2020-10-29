package poppet.coder.jackson

import cats.implicits._
import com.fasterxml.jackson.databind.JsonNode
import org.scalatest.FreeSpec
import poppet.coder.CoderFixture
import poppet.coder.CoderFixture.A
import poppet.coder.jackson.all._

class JacksonCoderSpec extends FreeSpec with CoderFixture {
    "Jackson coder should parse" - {
        "request and response data structures" in {
            assertExchangeCoder[JsonNode]
        }
        "custom data structures" in {
            assertCustomCoder[JsonNode, Unit](())
            assertCustomCoder[JsonNode, Int](intExample)
            assertCustomCoder[JsonNode, String](stringExample)
            assertCustomCoder[JsonNode, A](caseClassExample)
        }
    }
}
