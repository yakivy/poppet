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
    val drequest: Coder[Array[Byte], Future[Request[JsValue]]] =
        coderToFutureCoder(readsToCoder(RqF).compose(bytesToJsonCoder))
    val erequest: Coder[Request[JsValue], Future[Array[Byte]]] =
        coderToFutureCoder(jsonToBytesCoder.compose(writesToCoder(RqF)))
    val dresponse: Coder[Array[Byte], Future[Response[JsValue]]] =
        coderToFutureCoder(readsToCoder(RsF).compose(bytesToJsonCoder))
    val eresponse: Coder[Response[JsValue], Future[Array[Byte]]] =
        coderToFutureCoder(jsonToBytesCoder.compose(writesToCoder(RsF)))
}

object PlayCoder {
    implicit val RqF: Format[Request[JsValue]] = Json.format[Request[JsValue]]
    implicit val RsF: Format[Response[JsValue]] = Json.format[Response[JsValue]]
}