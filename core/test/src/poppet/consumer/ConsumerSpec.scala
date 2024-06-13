package poppet.consumer

import cats._
import org.scalatest.freespec.AnyFreeSpec
import poppet.codec.upickle.json.all._
import poppet.consumer.core.ConsumerProcessor
import poppet.core.Request
import poppet.core.Response
import ujson.Value
import upickle.default._

class ConsumerSpec extends AnyFreeSpec {
    trait A {
        def a(p0: String): String
    }
    "Consumer" - {
        val consumerProcessor = new ConsumerProcessor[Id, Value, A] {
            override def apply(
                client: Request[Value] => Id[Response[Value]], fh: FailureHandler[Id]
            ): A = new A {
                override def a(p0: String): String = client(Request("A", "a", Map("p0" -> writeJs(p0)))).value.str
            }
        }
        "should delegate calls correctly" in {
            val c = new Consumer[Id, Value, A](
                request => {
                    val result = read[String](request.arguments("p0")) + " response"
                    Response(writeJs(result))
                },
                FailureHandler.throwing,
                consumerProcessor
            ).service
            assert(c.a("request") == "request response")
        }
    }
}
