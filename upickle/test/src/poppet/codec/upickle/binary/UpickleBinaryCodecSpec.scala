package poppet.codec.upickle.binary

import org.scalatest.freespec.AnyFreeSpec
import poppet.codec.CodecSpec
import poppet.codec.CodecSpec.A
import poppet.codec.upickle.binary.all._
import upack.Msg
import upickle.default._

class UpickleBinaryCodecSpec extends AnyFreeSpec with CodecSpec {
    "Upickle binary codec should parse" - {
        "request and response data structures" in {
            assertExchangeCodec[Msg]
        }
        "custom data structures" in {
            implicit val RW: ReadWriter[A] = macroRW[A]
            assertCustomCodec[Msg, Unit](())
            assertCustomCodec[Msg, Int](intExample)
            assertCustomCodec[Msg, String](stringExample)
            assertCustomCodec[Msg, A](caseClassExample)
        }
    }
}
