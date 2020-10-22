package poppet.provider

import cats.Id
import org.scalatest.FreeSpec
import poppet.core.Coder
import poppet.core.ExchangeCoder
import poppet.core.Request
import poppet.core.Response
import poppet.provider.core.MethodProcessor

class ProviderSpec extends FreeSpec {
    "Provider should delegates calls correctly" in {
        val c = Provider[Id](
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
            new ProviderProcessor[String, Id]("A", List(new MethodProcessor[String, Id](
                "a", List("p0"), request => request("p0") + " response"
            )))
        ).materialize()
        assert(c("A,a,request".getBytes).toList == "request response".getBytes.toList)
    }
}
