package poppet.coder.play

import org.scalatest.FreeSpec
import play.api.libs.json.JsValue
import play.api.libs.json.Json
import poppet.coder.CoderFixture
import poppet.coder.CoderFixture.A
import poppet.coder.play.all._

class PlayJsonCoderSpec extends FreeSpec with CoderFixture {
    "Play coder should parse" - {
        "request and response data structures" in {
            assertExchangeCoder[JsValue]
        }
        "custom data structures" in {
            implicit val F = Json.format[A]
            assertCustomCoder[JsValue, Unit](())
            assertCustomCoder[JsValue, Int](intExample)
            assertCustomCoder[JsValue, String](stringExample)
            assertCustomCoder[JsValue, A](caseClassExample)
        }
    }
}
