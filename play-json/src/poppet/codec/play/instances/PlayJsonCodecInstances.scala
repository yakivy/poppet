package poppet.codec.play.instances

import play.api.libs.json._
import poppet._
import poppet.core.Request
import poppet.core.Response
import scala.collection.Seq

trait PlayJsonCodecInstances {
    implicit val unitFormat: Format[Unit] = Format[Unit](_ => JsSuccess(()), _ => JsObject(Seq.empty))
    implicit val rqFormat: Format[Request[JsValue]] = Json.format[Request[JsValue]]
    implicit val rsFormat: Format[Response[JsValue]] = Json.format[Response[JsValue]]

    implicit def playJsonReadsToCodec[A: Reads]: Codec[JsValue, A] = a => implicitly[Reads[A]].reads(a).asEither
        .left.map(f => new CodecFailure(f.headOption.map(e => s"${e._1} ${e._2}").getOrElse("Codec failure"), a))

    implicit def playJsonWritesToCodec[A: Writes]: Codec[A, JsValue] = a => Right(implicitly[Writes[A]].writes(a))
}
