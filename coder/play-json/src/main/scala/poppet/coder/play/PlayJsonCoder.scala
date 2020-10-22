package poppet.coder.play

import cats.Applicative
import play.api.libs.json.Format
import play.api.libs.json.JsValue
import play.api.libs.json.Json
import poppet.all._
import poppet.coder.play.PlayJsonCoder._
import poppet.coder.play.all._

case class PlayJsonCoder[F[_] : Applicative]() extends ExchangeCoder[JsValue, F] {
    val irequest: Coder[Array[Byte], F[Request[JsValue]]] =
        a => Applicative[F].pure((BytesToJsonCoder andThen readsToCoder(RqF)) (a))
    val brequest: Coder[Request[JsValue], F[Array[Byte]]] =
        a => Applicative[F].pure((writesToCoder(RqF) andThen JsonToBytesCoder) (a))
    val iresponse: Coder[Array[Byte], F[Response[JsValue]]] =
        a => Applicative[F].pure((BytesToJsonCoder andThen readsToCoder(RsF)) (a))
    val bresponse: Coder[Response[JsValue], F[Array[Byte]]] =
        a => Applicative[F].pure((JsonToBytesCoder compose writesToCoder(RsF)) (a))
}

object PlayJsonCoder {
    private val RqF: Format[Request[JsValue]] = Json.format[Request[JsValue]]
    private val RsF: Format[Response[JsValue]] = Json.format[Response[JsValue]]
    private val BytesToJsonCoder: Coder[Array[Byte], JsValue] = Json.parse
    private val JsonToBytesCoder: Coder[JsValue, Array[Byte]] = Json.toBytes
}