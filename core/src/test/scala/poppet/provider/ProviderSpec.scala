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
        implicit val fh: FailureHandler[MethodProcessor[String, Id]] = throw _
        val c = Provider[String, Id].apply(
            new ProviderProcessor[String, Id]("A", List(new MethodProcessor[String, Id](
                "a", List("p0"), request => request("p0") + " response"
            )))
        ).materialize()
        assert(c("A,a,request") == "request response")
    }
}
