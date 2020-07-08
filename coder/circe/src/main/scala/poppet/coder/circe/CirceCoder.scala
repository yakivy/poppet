package poppet.coder.circe

import cats.Applicative
import io.circe.Json
import io.circe.Printer
import io.circe.generic.auto._
import io.circe.jawn.JawnParser
import io.circe.syntax._
import poppet.coder.Coder
import poppet.coder.ExchangeCoder
import poppet.coder.circe.CirceCoder._
import poppet.dto.Request
import poppet.dto.Response

case class CirceCoder[F[_]]()(implicit A: Applicative[F]) extends ExchangeCoder[Array[Byte], Json, F] {
    val drequest: Coder[Array[Byte], F[Request[Json]]] =
        a => A.pure(Parser.parseByteArray(a).fold[Json](throw _, identity)
            .as[Request[Json]].fold(throw _, identity))
    val erequest: Coder[Request[Json], F[Array[Byte]]] =
        a => A.pure(Printer.noSpaces.printToByteBuffer(a.asJson).array())
    val dresponse: Coder[Array[Byte], F[Response[Json]]] =
        a => A.pure(Parser.parseByteArray(a).fold[Json](throw _, identity)
            .as[Response[Json]].fold(throw _, identity))
    val eresponse: Coder[Response[Json], F[Array[Byte]]] =
        a => A.pure(Printer.noSpaces.printToByteBuffer(a.asJson).array())
}

object CirceCoder {
    private val Parser = new JawnParser
}