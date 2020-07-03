package poppet.coder.jackson

import cats.Id
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import poppet.coder.Coder
import poppet.coder.ExchangeCoder
import poppet.coder.jackson.instances._
import poppet.dto.Request
import poppet.dto.Response

case class JacksonCoder()(
    implicit om: ObjectMapper,
    requestCoder: Coder[JsonNode, Request[JsonNode]], responseCoder: Coder[JsonNode, Response[JsonNode]],
) extends ExchangeCoder[Array[Byte], JsonNode, Id] {
    val drequest: Coder[Array[Byte], Id[Request[JsonNode]]] =
        requestCoder.compose(bytesToJsonCoder)
    val erequest: Coder[Request[JsonNode], Id[Array[Byte]]] =
        jsonToBytesCoder.compose(anyToJsonCoder[Request[JsonNode]])
    val dresponse: Coder[Array[Byte], Id[Response[JsonNode]]] =
        responseCoder.compose(bytesToJsonCoder)
    val eresponse: Coder[Response[JsonNode], Id[Array[Byte]]] =
        jsonToBytesCoder.compose(anyToJsonCoder[Response[JsonNode]])
}