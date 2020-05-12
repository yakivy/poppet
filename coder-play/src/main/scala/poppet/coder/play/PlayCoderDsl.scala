package poppet.coder.play

import play.api.libs.json.JsValue
import poppet.coder.BiCoder
import poppet.coder.play.instances._
import scala.concurrent.Future

trait PlayCoderDsl {
    val PlayBiCoder = BiCoder[Array[Byte], JsValue, Future](
        coderToFutureCoder(bytesToJsonCoder),
        coderToFutureCoder(jsonToBytesCoder),
    )
}
