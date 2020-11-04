package poppet.provider

import cats.Id
import org.scalatest.FreeSpec
import poppet.core.Request
import poppet.core.Response
import poppet.provider.core.MethodProcessor

class ProviderSpec extends FreeSpec {
    "Provider should delegates calls correctly" in {
        implicit val qcoder: Coder[String, Request[String]] =
            a => {
                val parts = a.split(",")
                Request(parts(0), parts(1), parts.drop(2).zipWithIndex.map(p => s"p${p._2}" -> p._1).toMap)
            }
        implicit val scoder: Coder[Response[String], String] = a => a.value
        implicit val fh: FailureHandler[Id[Map[String, String] => cats.Id[String]]] = throw _
        val c = new Provider[String, Id](List(new MethodProcessor[String, Id](
            "A", "a", List("p0"), request => request("p0") + " response"
        )))
        assert(c("A,a,request") == "request response")
    }
}
