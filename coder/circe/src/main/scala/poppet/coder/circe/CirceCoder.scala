package poppet.coder.circe

import cats.Applicative
import io.circe.Json
import io.circe.Printer
import io.circe.generic.auto._
import io.circe.jawn.JawnParser
import io.circe.syntax._
import java.nio.ByteBuffer
import poppet.coder.circe.CirceCoder._
import poppet.core.Coder
import poppet.core.ExchangeCoder
import poppet.core.Request
import poppet.core.Response

case class CirceCoder[F[_] : Applicative]() extends ExchangeCoder[Json, F] {
    val irequest: Coder[Array[Byte], F[Request[Json]]] =
        a => Applicative[F].pure(Parser.parseByteArray(a).fold[Json](throw _, identity)
            .as[Request[Json]].fold(throw _, identity))
    val brequest: Coder[Request[Json], F[Array[Byte]]] =
        a => Applicative[F].pure(toArray(Printer.noSpaces.printToByteBuffer(a.asJson)))
    val iresponse: Coder[Array[Byte], F[Response[Json]]] =
        a => Applicative[F].pure(Parser.parseByteArray(a).fold[Json](throw _, identity)
            .as[Response[Json]].fold(throw _, identity))
    val bresponse: Coder[Response[Json], F[Array[Byte]]] =
        a => Applicative[F].pure(toArray(Printer.noSpaces.printToByteBuffer(a.asJson)))
}

object CirceCoder {
    private val Parser = new JawnParser
    private def toArray(buffer: ByteBuffer): Array[Byte] = {
        val result = new Array[Byte](buffer.remaining())
        buffer.get(result)
        result
    }
}