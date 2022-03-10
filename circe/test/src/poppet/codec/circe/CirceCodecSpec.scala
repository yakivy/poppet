package poppet.codec.circe

import io.circe.Json
import io.circe.generic.auto._
import org.scalatest.freespec.AnyFreeSpec
import poppet.codec.CodecSpec
import poppet.codec.CodecSpec.A
import poppet.codec.circe.all._

class CirceCodecSpec extends AnyFreeSpec with CodecSpec {
    "Circe codec should parse" - {
        "request and response data structures" in {
            assertExchangeCodec[Json]
        }
        "custom data structures" in {
            assertCustomCodec[Json, Unit](())
            assertCustomCodec[Json, Int](intExample)
            assertCustomCodec[Json, String](stringExample)
            assertCustomCodec[Json, A](caseClassExample)
        }
    }
}
