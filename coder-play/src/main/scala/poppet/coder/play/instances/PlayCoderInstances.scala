package poppet.coder.play.instances

import play.api.libs.json.JsValue
import play.api.libs.json.Json
import poppet.coder.Coder
import scala.concurrent.Future

trait PlayCoderInstances {
    implicit val bytesToJsonCoder: Coder[Array[Byte], JsValue] = Json.parse
    implicit val jsonToBytesCoder: Coder[JsValue, Array[Byte]] = Json.toBytes
    implicit def coderToFutureCoder[A, B](implicit coder: Coder[A, B]): Coder[A, Future[B]] =
        v => Future.successful(coder(v))
}
