package poppet.coder.play

import play.api.libs.json.Format
import play.api.libs.json.JsValue
import play.api.libs.json.Json
import poppet.coder.Coder
import poppet.coder.ExchangeCoder
import poppet.coder.play.PlayCoder._
import poppet.coder.play.instances._
import poppet.dto.Request
import poppet.dto.Response
import scala.concurrent.Future

case class PlayCoder() extends ExchangeCoder[Array[Byte], JsValue, Future] {
    override def request: Coder[Array[Byte], Future[Request[JsValue]]] =
        r => Future.successful(bytesToJsonCoder(r).as[Request[JsValue]])
    override def response: Coder[Response[JsValue], Future[Array[Byte]]] =
        r => Future.successful(jsonToBytesCoder(Json.toJson(r)))
}

object PlayCoder {
    implicit val RqF: Format[Request[JsValue]] = Json.format[Request[JsValue]]
    implicit val RsF: Format[Response[JsValue]] = Json.format[Response[JsValue]]
}