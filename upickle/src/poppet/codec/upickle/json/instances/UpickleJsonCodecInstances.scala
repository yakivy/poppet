package poppet.codec.upickle.json.instances

import poppet._
import poppet.core.Request
import poppet.core.Response
import ujson.ParsingFailedException
import ujson.Value
import upickle.core.Abort
import upickle.default._

trait UpickleJsonCodecInstances {
    implicit val upickleRequestJsonRW: ReadWriter[Request[Value]] = macroRW[Request[Value]]
    implicit val upickleResponseJsonRW: ReadWriter[Response[Value]] = macroRW[Response[Value]]

    implicit def upickleReaderToJsonCodec[A: Reader]: Codec[Value, A] = a =>
        try Right(read[A](a))
        catch { case e: Abort => Left(new CodecFailure(e.getMessage, a, e)) }

    implicit def upickleWriterToJsonCodec[A: Writer]: Codec[A, Value] = a => Right(writeJs(a))
}
