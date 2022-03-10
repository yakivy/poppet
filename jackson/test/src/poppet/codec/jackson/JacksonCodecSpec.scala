package poppet.codec.jackson

import com.fasterxml.jackson.databind.JsonNode
import org.scalatest.freespec.AnyFreeSpec
import poppet.codec.CodecSpec
import poppet.codec.CodecSpec.A
import poppet.codec.jackson.all._

class JacksonCodecSpec extends AnyFreeSpec with CodecSpec {
    "Jackson codec should parse" - {
        "request and response data structures" in {
            assertExchangeCodec[JsonNode]
        }
        "custom data structures" in {
            assertCustomCodec[JsonNode, Unit](())
            assertCustomCodec[JsonNode, Int](intExample)
            assertCustomCodec[JsonNode, Long](1L)
            assertCustomCodec[JsonNode, String](stringExample)
            assertCustomCodec[JsonNode, A](caseClassExample)
        }
    }
}
