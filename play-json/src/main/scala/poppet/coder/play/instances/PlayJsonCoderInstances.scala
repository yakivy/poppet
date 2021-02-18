package poppet.coder.play.instances

import cats.Applicative
import cats.Monad
import cats.implicits._
import play.api.libs.json.Format
import play.api.libs.json.JsObject
import play.api.libs.json.JsSuccess
import play.api.libs.json.JsValue
import play.api.libs.json.Json
import play.api.libs.json.Reads
import play.api.libs.json.Writes
import poppet.coder.instances.CoderInstances
import poppet.core._
import scala.collection.Seq

trait PlayJsonCoderInstances extends CoderInstances {
    implicit val unitFormat: Format[Unit] = Format[Unit](_ => JsSuccess(()), _ => JsObject(Seq.empty))
    implicit val rqFormat: Format[Request[JsValue]] = Json.format[Request[JsValue]]
    implicit val rsFormat: Format[Response[JsValue]] = Json.format[Response[JsValue]]

    implicit def readsToCoder[A, F[_] : Monad](
        implicit r: Reads[A], fh: FailureHandler[F[A]]
    ): Coder[JsValue, F[A]] = a => Monad[F].pure(r.reads(a).asEither).flatMap {
        case Right(value) => Monad[F].pure(value)
        case Left(value) => fh(new DecodingFailure(s"Can't decode model: $value", a))
    }
    implicit def writesToCoder[A, F[_] : Applicative](
        implicit w: Writes[A]
    ): Coder[A, F[JsValue]] = a => Applicative[F].pure(w.writes(a))
}
