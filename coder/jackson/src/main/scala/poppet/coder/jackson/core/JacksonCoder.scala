package poppet.coder.jackson.core

import cats.Applicative
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import poppet.all._
import poppet.coder.jackson.all._
import poppet.coder.jackson.core.JacksonCoder._

case class JacksonCoder[F[_] : Applicative]()(
    implicit om: ObjectMapper,
    rqCoder: Coder[JsonNode, Request[JsonNode]],
    rsCoder: Coder[JsonNode, Response[JsonNode]],
) extends ExchangeCoder[JsonNode, F] {
    val irequest: Coder[Array[Byte], F[Request[JsonNode]]] =
        a => Applicative[F].pure((bytesToJsonCoder andThen rqCoder) (a))
    val brequest: Coder[Request[JsonNode], F[Array[Byte]]] =
        a => Applicative[F].pure((anyToJsonCoder[Request[JsonNode]] andThen jsonToBytesCoder) (a))
    val iresponse: Coder[Array[Byte], F[Response[JsonNode]]] =
        a => Applicative[F].pure((bytesToJsonCoder andThen rsCoder) (a))
    val bresponse: Coder[Response[JsonNode], F[Array[Byte]]] =
        a => Applicative[F].pure((anyToJsonCoder[Response[JsonNode]] andThen jsonToBytesCoder) (a))
}

object JacksonCoder {
    private def bytesToJsonCoder(implicit om: ObjectMapper): Coder[Array[Byte], JsonNode] =
        a => om.readTree(a)
    private def jsonToBytesCoder(implicit om: ObjectMapper): Coder[JsonNode, Array[Byte]] =
        a => om.writeValueAsBytes(a)
}