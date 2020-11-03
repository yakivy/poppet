package poppet.consumer

import cats.Id
import org.scalatest.FreeSpec
import poppet.core.Request
import poppet.core.Response

class ConsumerSpec extends FreeSpec {
    "Consumer should delegates calls correctly" in {
        trait A {
            def a(p0: String): String
        }
        implicit val qcoder: Coder[Request[String], String] =
            a => (a.service :: a.method :: a.arguments.values.toList).mkString(",")
        implicit val scoder: Coder[String, Response[String]] = a => Response(a)
        val c = Consumer[String, Id].apply(
            request => {
                val parts = new String(request).split(",")
                require(parts(0) == "A")
                require(parts(1) == "a")
                (parts(2) + " response")
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
