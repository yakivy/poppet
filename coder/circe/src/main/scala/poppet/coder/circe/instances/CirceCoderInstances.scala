package poppet.coder.circe.instances

import cats.FlatMap
import cats.Id
import cats.implicits._
import cats.syntax._
import io.circe.Decoder
import io.circe.DecodingFailure
import io.circe.Encoder
import io.circe.Json
import io.circe.ParsingFailure
import io.circe.Printer
import io.circe.jawn.JawnParser
import java.nio.ByteBuffer
import poppet.all._
import poppet.instances.CoderInstances

trait CirceCoderInstances extends CoderInstances {
    private val Parser = new JawnParser
    private def toArray(buffer: ByteBuffer): Array[Byte] = {
        val result = new Array[Byte](buffer.remaining())
        buffer.get(result)
        result
    }
    implicit def fromBytesExchangeCoder[A, F[_] : FlatMap](
        implicit mc: ModelCoder[Json, F[A]], eh: ErrorHandler[Either[ParsingFailure, Json], F[Json]]
    ): ExchangeCoder[Array[Byte], F[A]] = a => eh(Parser.parseByteArray(a)).flatMap(mc)
    implicit def toBytesExchangeCoder[A, F[_] : FlatMap](
        implicit mc: ModelCoder[A, F[Json]], eh: ErrorHandler[Array[Byte], F[Array[Byte]]]
    ): ExchangeCoder[A, F[Array[Byte]]] =
        a => mc(a).flatMap(b => eh(toArray(Printer.noSpaces.printToByteBuffer(b))))

    implicit def decoderToModelCoder[A, F[_]](
        implicit eh: ErrorHandler[Either[DecodingFailure, A], F[A]], d: Decoder[A]
    ): ModelCoder[Json, F[A]] = a => eh(d(a.hcursor))
    implicit def encoderToModelCoder[A, F[_]](
        implicit eh: ErrorHandler[Json, F[Json]], e: Encoder[A]
    ): ModelCoder[A, F[Json]] = a => eh(e(a))

    implicit def idParsingErrorHandler[A]: ErrorHandler[Either[ParsingFailure, A], Id[A]] =
        _.valueOr(f => throw new Error(f.message, f.underlying))
    implicit def idDecodingErrorHandler[A]: ErrorHandler[Either[DecodingFailure, A], Id[A]] =
        _.valueOr(f => throw new Error("Decoding error: ", f))
}
