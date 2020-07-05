package poppet.provider

import cats.Id
import org.scalatest.FreeSpec
import poppet.coder.Coder
import poppet.coder.ExchangeCoder
import poppet.dto.Request
import poppet.dto.Response

class ProviderSpec extends FreeSpec {
    "Provider should delegates calls correctly" in {
        trait A {
            def a(a: String): String
        }
        val c = Provider(
            new Server[
                Map[String, String], Id, Map[String, String], Map[String, String],
                Map[String, String] => Map[String, String]
            ] {
                override def buildRequest(request: Map[String, String]): Id[Map[String, String]] = request
                override def buildResponse(response: Map[String, String]): Id[Map[String, String]] = response
                override def materialize(
                    f: Map[String, String] => Id[Map[String, String]]
                ): Map[String, String] => Map[String, String] = f
            })(
            new ExchangeCoder[Map[String, String], Map[String, String], Id] {
                override def drequest: Coder[Map[String, String], Id[Request[Map[String, String]]]] =
                    a => Request(a("service"), a("method"), Map("a" -> Map("value" -> a("a"))))
                override def erequest: Coder[Request[Map[String, String]], Id[Map[String, String]]] =
                    a => Map("service" -> a.service, "method" -> a.method, "a" -> a.arguments.head._2("value"))
                override def dresponse: Coder[Map[String, String], Id[Response[Map[String, String]]]] =
                    a => Response(a)
                override def eresponse: Coder[Response[Map[String, String]], Id[Map[String, String]]] =
                    a => a.value
            })(
            new ProviderProcessor[Map[String, String], Id]("A", List(new MethodProcessor[Map[String, String], Id](
                "a", List("a"), request => Map("value" -> (request("a")("value") + " response"))
            )))
        ).materialize()
        assert(c(Map("service" -> "A", "method" -> "a", "a" -> "request"))("value") == "request response")
    }
}
