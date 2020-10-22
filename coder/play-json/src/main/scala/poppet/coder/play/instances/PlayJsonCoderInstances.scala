package poppet.coder.play.instances

import play.api.libs.json.Format
import play.api.libs.json.JsObject
import play.api.libs.json.JsSuccess
import play.api.libs.json.JsValue
import play.api.libs.json.Reads
import play.api.libs.json.Writes
import poppet.Coder
import poppet.instances.CoderInstances

trait PlayJsonCoderInstances extends CoderInstances {
    implicit def writesToCoder[A](implicit writes: Writes[A]): Coder[A, JsValue] = writes.writes(_)
    implicit def readsToCoder[A](implicit reads: Reads[A]): Coder[JsValue, A] = reads.reads(_).asEither.right.get
    implicit val unitFormat = Format[Unit](_ => JsSuccess(()), _ => JsObject(Seq.empty))
}
