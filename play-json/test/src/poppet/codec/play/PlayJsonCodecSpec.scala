package poppet.codec.play

import org.scalatest.freespec.AnyFreeSpec
import play.api.libs.json.JsValue
import play.api.libs.json.Json
import poppet.codec.CodecSpec
import poppet.codec.CodecSpec.A
import poppet.codec.play.all._

class PlayJsonCodecSpec extends AnyFreeSpec with CodecSpec {
    "Play codec should parse" - {
        "custom data structures" in {
            implicit val F = Json.format[A]
            assertCustomCodec[JsValue, Unit](())
            assertCustomCodec[JsValue, Int](intExample)
            assertCustomCodec[JsValue, String](stringExample)
            assertCustomCodec[JsValue, A](caseClassExample)
        }
    }
}
