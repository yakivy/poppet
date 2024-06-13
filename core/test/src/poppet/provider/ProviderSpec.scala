package poppet.provider

import cats._
import org.scalatest.freespec.AnyFreeSpec
import poppet.codec.upickle.json.all._
import poppet.core.Request
import poppet.core.Response
import poppet.provider.core.MethodProcessor
import ujson.Value
import upickle.default._

class ProviderSpec extends AnyFreeSpec {
    "Provider" - {
        def providerProcessors[F[_]: Applicative] = List(new MethodProcessor[F, Value](
            "A", "a", List("p0"),
            request => Applicative[F].pure(writeJs(read[String](request("p0")) + " response"))
        ))
        val validRequest = Request("A", "a", Map("p0" -> writeJs("request")))
        val validResponse = Response(writeJs("request response"))
        "should delegates calls correctly" in {
            val p = new Provider[Id, Value](
                FailureHandler.throwing,
                providerProcessors,
            )
            assert(p.apply(validRequest) == validResponse)
        }
        "should raise a failure if processor is not found" in {
            type Response[A] = Either[Failure, A]
            val p = new Provider[Response, Value](
                new FailureHandler[Response] {
                    override def apply[A](f: Failure): Response[A] = Left(f)
                },
                providerProcessors,
            )
            assert(p.apply(Request(
                "B", "a", Map("p0" -> writeJs("request"))
            )).left.map(_.getMessage) == Left(
                "Requested processor B is not in [A]. Make sure that desired service is provided and up to date."
            ))
            assert(p.apply(Request(
                "A", "b", Map("p0" -> writeJs("request"))
            )).left.map(_.getMessage) == Left(
                "Requested processor A.b is not in A.[a]. Make sure that desired service is provided and up to date."
            ))
            assert(p.apply(Request(
                "A", "a", Map("p1" -> writeJs("request"))
            )).left.map(_.getMessage) == Left(
                "Requested processor A.a(p1) is not in A.a[(p0)]. Make sure that desired service is provided and up to date."
            ))
        }
    }
}
