package poppet.coder.jackson

import cats.Applicative
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import poppet.coder.Coder
import poppet.coder.ExchangeCoder
import poppet.coder.jackson.JacksonCoder._
import poppet.coder.jackson.instances._
import poppet.dto.Request
import poppet.dto.Response

case class JacksonCoder[F[_] : Applicative]()(
    implicit om: ObjectMapper,
    rqCoder: Coder[JsonNode, Request[JsonNode]],
    rsCoder: Coder[JsonNode, Response[JsonNode]],
) extends ExchangeCoder[Array[Byte], JsonNode, F] {
    val drequest: Coder[Array[Byte], F[Request[JsonNode]]] =
        a => Applicative[F].pure((bytesToJsonCoder andThen rqCoder) (a))
    val erequest: Coder[Request[JsonNode], F[Array[Byte]]] =
        a => Applicative[F].pure((anyToJsonCoder[Request[JsonNode]] andThen jsonToBytesCoder) (a))
    val dresponse: Coder[Array[Byte], F[Response[JsonNode]]] =
        a => Applicative[F].pure((bytesToJsonCoder andThen rsCoder) (a))
    val eresponse: Coder[Response[JsonNode], F[Array[Byte]]] =
        a => Applicative[F].pure((anyToJsonCoder[Response[JsonNode]] andThen jsonToBytesCoder) (a))
}

object JacksonCoder {
    private def bytesToJsonCoder(implicit om: ObjectMapper): Coder[Array[Byte], JsonNode] =
        a => om.readTree(a)
    private def jsonToBytesCoder(implicit om: ObjectMapper): Coder[JsonNode, Array[Byte]] =
        a => om.writeValueAsBytes(a)
}