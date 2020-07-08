package poppet.coder.play

import cats.Id
import org.scalatest.FreeSpec
import play.api.libs.json.Json
import poppet.coder.CoderFixture
import poppet.coder.CoderFixture.A
import poppet.coder.play.all._

class PlayJsonCoderSpec extends FreeSpec with CoderFixture {
    "Play coder should parse" - {
        val coder = PlayJsonCoder[Id]()
        "request and response data structures" in {
            assertExchangeCoder(coder)
        }
        "custom data structures" in {
            implicit val F = Json.format[A]
            assertCustomCoder(coder)(())
            assertCustomCoder(coder)(intExample)
            assertCustomCoder(coder)(stringExample)
            assertCustomCoder(coder)(caseClassExample)
        }
    }
}
