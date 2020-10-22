package poppet.consumer

import cats.Id
import org.scalatest.FreeSpec
import poppet.consumer.Client
import poppet.consumer.ConsumerProcessor
import poppet.core.Coder
import poppet.core.ExchangeCoder
import poppet.core.Request
import poppet.core.Response

class ConsumerSpec extends FreeSpec {
    "Consumer should delegates calls correctly" in {
        trait A {
            def a(p0: String): String
        }
        val c = Consumer[Id](
            request => {
                val parts = new String(request).split(",")
                require(parts(0) == "A")
                require(parts(1) == "a")
                (parts(2) + " response").getBytes
            },
            new ExchangeCoder[String, Id] {
                override def irequest: Coder[Array[Byte], Id[Request[String]]] = a => {
                    val parts = new String(a).split(",")
                    Request(parts(0), parts(1), parts.drop(2).zipWithIndex.map(p => s"p${p._2}" -> p._1).toMap)
                }
                override def brequest: Coder[Request[String], Id[Array[Byte]]] =
                    a => (a.service :: a.method :: a.arguments.values.toList).mkString(",").getBytes
                override def iresponse: Coder[Array[Byte], Id[Response[String]]] =
                    a => Response(new String(a))
                override def bresponse: Coder[Response[String], Id[Array[Byte]]] =
                    a => a.value.getBytes
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
