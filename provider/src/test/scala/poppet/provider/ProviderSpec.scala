package poppet.provider

import cats.Id
import org.scalatest.FreeSpec
import poppet.all._
import poppet.provider.core.MethodProcessor

class ProviderSpec extends FreeSpec {
    "Provider should delegates calls correctly" in {
        implicit val iqcoder: ExchangeCoder[Array[Byte], Request[String]] =
            a => {
                val parts = new String(a).split(",")
                Request(parts(0), parts(1), parts.drop(2).zipWithIndex.map(p => s"p${p._2}" -> p._1).toMap)
            }
        implicit val bscoder: ExchangeCoder[Response[String], Array[Byte]] =
            a => a.value.getBytes
        implicit val eh: ErrorHandler[MethodProcessor[String, Id]] = throw _
        val c = Provider[String, Id].apply(
            new ProviderProcessor[String, Id]("A", List(new MethodProcessor[String, Id](
                "a", List("p0"), request => request("p0") + " response"
            )))
        ).materialize()
        assert(c("A,a,request".getBytes).toList == "request response".getBytes.toList)
    }
}
