package poppet.coder.play.instances

import cats.Functor
import play.api.libs.json.Format
import play.api.libs.json.JsObject
import play.api.libs.json.JsSuccess
import play.api.libs.json.JsValue
import play.api.libs.json.Reads
import play.api.libs.json.Writes
import poppet.coder.Coder
import poppet.coder.instances.CoderInstances
import scala.concurrent.Future

trait PlayJsonCoderInstances extends CoderInstances {
    implicit def coderToFutureCoder[A, B](implicit coder: Coder[A, B]): Coder[A, Future[B]] =
        a => Future.successful(coder(a))
    implicit def coderToFuturesCoder[A, B](
        implicit coder: Coder[A, B], F: Functor[Future]
    ): Coder[Future[A], Future[B]] = a => F.fmap(a)(ar => coder(ar))
    implicit def writesToCoder[A](implicit writes: Writes[A]): Coder[A, JsValue] = writes.writes(_)
    implicit def readsToCoder[A](implicit reads: Reads[A]): Coder[JsValue, A] = reads.reads(_).asEither.right.get
    implicit val unitFormat = Format[Unit](_ => JsSuccess(()), _ => JsObject(Seq.empty))
}
