package poppet.codec.upickle.binary.instances

import poppet._
import poppet.core.Request
import poppet.core.Response
import upack.Msg
import upickle.default._

trait UpickleBinaryCodecInstancesLp0 {
    implicit def upickleReaderToByteCodec[A: Reader]: Codec[Msg, A] = a =>
        try Right(readBinary[A](a))
        catch { case e: Msg.InvalidData => Left(new CodecFailure(e.getMessage, a, e)) }
}

trait UpickleBinaryCodecInstances extends UpickleBinaryCodecInstancesLp0 {
    implicit val upickleRequestBinaryRW: ReadWriter[Request[Msg]] = macroRW[Request[Msg]]
    implicit val upickleResponseBinaryRW: ReadWriter[Response[Msg]] = macroRW[Response[Msg]]

    implicit def upickleWriterToByteCodec[A: Writer]: Codec[A, Msg] = a => Right(writeMsg(a))
}
