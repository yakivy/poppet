package poppet.consumer

import cats.Id
import org.scalatest.FreeSpec
import poppet.core.ExchangeCoder
import poppet.core.Request
import poppet.core.Response

class ConsumerSpec extends FreeSpec {
    "Consumer should delegates calls correctly" in {
        trait A {
            def a(p0: String): String
        }
        implicit val bqcoder: ExchangeCoder[Request[String], Array[Byte]] =
            a => (a.service :: a.method :: a.arguments.values.toList).mkString(",").getBytes
        implicit val iscoder: ExchangeCoder[Array[Byte], Response[String]] =
            a => Response(new String(a))
        val c = Consumer[String, Id].apply(
            request => {
                val parts = new String(request).split(",")
                require(parts(0) == "A")
                require(parts(1) == "a")
                (parts(2) + " response").getBytes
            })(
            new ConsumerProcessor[String, Id, A] {
                override def process(client: Request[String] => Id[Response[String]]): A = new A {
                    override def a(p0: String): String = client(Request("A", "a", Map("p0" -> p0))).value
                }
            }
        ).materialize()
        assert(c.a("request") == "request response")
    }
}
