package poppet.codec.upickle.json

import org.scalatest.freespec.AnyFreeSpec
import poppet.codec.CodecSpec
import poppet.codec.CodecSpec.A
import poppet.codec.upickle.json.all._
import ujson.Value
import upickle.default._

class UpickleJsonCodecSpec extends AnyFreeSpec with CodecSpec {
    "Upickle binary codec should parse" - {
        "request and response data structures" in {
            assertExchangeCodec[Value]
        }
        "custom data structures" in {
            implicit val RW: ReadWriter[A] = macroRW[A]
            assertCustomCodec[Value, Unit](())
            assertCustomCodec[Value, Int](intExample)
            assertCustomCodec[Value, String](stringExample)
            assertCustomCodec[Value, A](caseClassExample)
        }
    }
}
