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
                    val requestP = read[Request[Value]](request)
                    val result = read[String](requestP.arguments("p0")) + " response"
                    writeJs(Response(writeJs(result)))
                },
                identity,
                FailureHandler.throwing,
                consumerProcessor
            ).service
            assert(c.a("request") == "request response")
        }
        "should peek in request to response function" in {
            val rq = Request[Value]("A", "a", Map("p0" -> "dummy"))
            val rs = Response[Value]("dummy")
            var peekRq = Option.empty[Request[Value]]
            var peekRs = Option.empty[Response[Value]]
            new Consumer[Id, Value, A](
                _ => writeJs(rs),
                f => rq => {
                    peekRq = Option(rq)
                    val rs = f(rq)
                    peekRs = Option(rs)
                    rs
                },
                FailureHandler.throwing,
                consumerProcessor
            ).service.a("dummy")
            assert(peekRq.contains(rq) && peekRs.contains(rs))
        }
    }
}
