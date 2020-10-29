package poppet.coder.play.instances

import cats.FlatMap
import cats.Id
import cats.implicits._
import play.api.libs.json.Format
import play.api.libs.json.JsObject
import play.api.libs.json.JsPath
import play.api.libs.json.JsSuccess
import play.api.libs.json.JsValue
import play.api.libs.json.Json
import play.api.libs.json.JsonValidationError
import play.api.libs.json.Reads
import play.api.libs.json.Writes
import poppet.all._
import poppet.instances.CoderInstances
import scala.collection.Seq

trait PlayJsonCoderInstances extends CoderInstances {
    implicit val unitFormat: Format[Unit] = Format[Unit](_ => JsSuccess(()), _ => JsObject(Seq.empty))
    implicit val rqFormat: Format[Request[JsValue]] = Json.format[Request[JsValue]]
    implicit val rsFormat: Format[Response[JsValue]] = Json.format[Response[JsValue]]

    implicit def fromBytesExchangeCoder[A, F[_] : FlatMap](
        implicit mc: ModelCoder[JsValue, F[A]], eh: ErrorHandler[JsValue, F[JsValue]]
    ): ExchangeCoder[Array[Byte], F[A]] = a => eh(Json.parse(a)).flatMap(mc)
    implicit def toBytesExchangeCoder[A, F[_] : FlatMap](
        implicit mc: ModelCoder[A, F[JsValue]], eh: ErrorHandler[Array[Byte], F[Array[Byte]]]
    ): ExchangeCoder[A, F[Array[Byte]]] = a => mc(a).flatMap(b => eh(Json.toBytes(b)))

    implicit def readsToModelCoder[A, F[_]](
        implicit eh: ErrorHandler[Either[Seq[(JsPath, Seq[JsonValidationError])], A], F[A]], r: Reads[A]
    ): ModelCoder[JsValue, F[A]] = a => eh(r.reads(a).asEither)
    implicit def writesToModelCoder[A, F[_]](
        implicit eh: ErrorHandler[JsValue, F[JsValue]], w: Writes[A]
    ): ModelCoder[A, F[JsValue]] = a => eh(w.writes(a))

    implicit def idErrorHandler[A]: ErrorHandler[Either[Seq[(JsPath, Seq[JsonValidationError])], A], Id[A]] =
        _.valueOr(f => throw new Error(s"Decoding error: $f"))
}
