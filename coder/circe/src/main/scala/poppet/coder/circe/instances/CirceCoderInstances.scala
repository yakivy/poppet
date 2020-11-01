package poppet.coder.circe.instances

import cats.Applicative
import cats.Functor
import cats.Monad
import cats.implicits._
import io.circe.Decoder
import io.circe.Encoder
import io.circe.Json
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
    implicit def fromBytesExchangeCoder[A, F[_] : Monad](
        implicit mc: ModelCoder[Json, F[A]], fh: FailureHandler[F[Json]]
    ): ExchangeCoder[Array[Byte], F[A]] = a => Applicative[F].pure(Parser.parseByteArray(a)).flatMap {
        case Right(value) => Applicative[F].pure(value)
        case Left(value) => fh(new Failure(value.message, value.underlying))
    }.flatMap(mc.apply)
    implicit def toBytesExchangeCoder[A, F[_] : Functor](
        implicit mc: ModelCoder[A, F[Json]]
    ): ExchangeCoder[A, F[Array[Byte]]] =
        a => mc(a).map(b => toArray(Printer.noSpaces.printToByteBuffer(b)))

    implicit def decoderToModelCoder[A, F[_] : Applicative](
        implicit d: Decoder[A], fh: FailureHandler[F[A]]
    ): ModelCoder[Json, F[A]] = a => d(a.hcursor) match {
        case Right(value) => Applicative[F].pure(value)
        case Left(value) => fh(new Failure(s"Decoding error: ${value.getMessage()}", value))
    }
    implicit def encoderToModelCoder[A, F[_] : Applicative](
        implicit e: Encoder[A]
    ): ModelCoder[A, F[Json]] = a => Applicative[F].pure(e(a))
}
