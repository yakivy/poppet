package poppet.consumer

import cats.Id
import org.scalatest.FreeSpec
import poppet.coder.Coder
import poppet.coder.ExchangeCoder
import poppet.dto.Request
import poppet.dto.Response

class ConsumerSpec extends FreeSpec {
    "Consumer should delegates calls correctly" in {
        trait A {
            def a(a: String): String
        }
        val c = Consumer(
            new Client[Map[String, String], Id, Map[String, String], Map[String, String]] {
                override def buildRequest(request: Map[String, String]): Id[Map[String, String]] = request
                override def buildResponse(response: Map[String, String]): Id[Map[String, String]] = response
                override def execute(request: Map[String, String]): Id[Map[String, String]] = {
                    require(request("service") == "A")
                    require(request("method") == "a")
                    Map("value" -> (request("a") + " response"))
                }
            })(
            new ExchangeCoder[Map[String, String], Map[String, String], Id] {
                override def drequest: Coder[Map[String, String], Id[Request[Map[String, String]]]] =
                    a => Request(a("service"), a("method"), a.view.mapValues(v => Map("value" -> v)).toMap)
                override def erequest: Coder[Request[Map[String, String]], Id[Map[String, String]]] =
                    a => Map(
                        "service" -> a.service,
                        "method" -> a.method,
                        "a" -> a.arguments("a")("value")
                    )
                override def dresponse: Coder[Map[String, String], Id[Response[Map[String, String]]]] =
                    a => Response(a)
                override def eresponse: Coder[Response[Map[String, String]], Id[Map[String, String]]] =
                    a => a.value
            })(
            new ConsumerProcessor[Map[String, String], Id, A](client => (a: String) => {
                client(Request("A", "a", Map("a" -> Map("value" -> a)))).value("value")
            })
        ).materialize()
        assert(c.a("request") == "request response")
    }
}
