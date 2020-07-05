package poppet.coder.play

import cats.Applicative
import play.api.libs.json.Format
import play.api.libs.json.JsValue
import play.api.libs.json.Json
import poppet.coder.Coder
import poppet.coder.ExchangeCoder
import poppet.coder.play.PlayJsonCoder._
import poppet.coder.play.instances._
import poppet.dto.Request
import poppet.dto.Response

case class PlayJsonCoder[F[_]]()(implicit A: Applicative[F]) extends ExchangeCoder[Array[Byte], JsValue, F] {
    val drequest: Coder[Array[Byte], F[Request[JsValue]]] =
        a => A.pure((BytesToJsonCoder andThen readsToCoder(RqF)) (a))
    val erequest: Coder[Request[JsValue], F[Array[Byte]]] =
        a => A.pure((writesToCoder(RqF) andThen JsonToBytesCoder) (a))
    val dresponse: Coder[Array[Byte], F[Response[JsValue]]] =
        a => A.pure((BytesToJsonCoder andThen readsToCoder(RsF)) (a))
    val eresponse: Coder[Response[JsValue], F[Array[Byte]]] =
        a => A.pure((JsonToBytesCoder compose writesToCoder(RsF)) (a))
}

object PlayJsonCoder {
    private val RqF: Format[Request[JsValue]] = Json.format[Request[JsValue]]
    private val RsF: Format[Response[JsValue]] = Json.format[Response[JsValue]]
    private val BytesToJsonCoder: Coder[Array[Byte], JsValue] = Json.parse
    private val JsonToBytesCoder: Coder[JsValue, Array[Byte]] = Json.toBytes
}