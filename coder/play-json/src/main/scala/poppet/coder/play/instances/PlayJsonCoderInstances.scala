package poppet.coder.play.instances

import cats.Applicative
import cats.Functor
import cats.Monad
import cats.implicits._
import play.api.libs.json.Format
import play.api.libs.json.JsObject
import play.api.libs.json.JsSuccess
import play.api.libs.json.JsValue
import play.api.libs.json.Json
import play.api.libs.json.Reads
import play.api.libs.json.Writes
import poppet.all._
import poppet.instances.CoderInstances
import scala.collection.Seq

trait PlayJsonCoderInstances extends CoderInstances {
    implicit val unitFormat: Format[Unit] = Format[Unit](_ => JsSuccess(()), _ => JsObject(Seq.empty))
    implicit val rqFormat: Format[Request[JsValue]] = Json.format[Request[JsValue]]
    implicit val rsFormat: Format[Response[JsValue]] = Json.format[Response[JsValue]]

    implicit def fromBytesExchangeCoder[A, F[_]](
        implicit mc: ModelCoder[JsValue, F[A]]
    ): ExchangeCoder[Array[Byte], F[A]] = a => mc.apply(Json.parse(a))
    implicit def toBytesExchangeCoder[A, F[_] : Functor](
        implicit mc: ModelCoder[A, F[JsValue]]
    ): ExchangeCoder[A, F[Array[Byte]]] = a => mc(a).map(Json.toBytes)

    implicit def readsToModelCoder[A, F[_] : Monad](
        implicit r: Reads[A], fh: FailureHandler[F[A]]
    ): ModelCoder[JsValue, F[A]] = a => Monad[F].pure(r.reads(a).asEither).flatMap {
        case Right(value) => Monad[F].pure(value)
        case Left(value) => fh(new Failure(s"Can't parse model: $value"))
    }
    implicit def writesToModelCoder[A, F[_] : Applicative](
        implicit w: Writes[A]
    ): ModelCoder[A, F[JsValue]] = a => Applicative[F].pure(w.writes(a))
}
