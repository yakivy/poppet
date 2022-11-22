package poppet.codec.jackson.instances

import cats.Applicative
import cats.Functor
import cats.implicits.*
import com.fasterxml.jackson.core.`type`.TypeReference
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import poppet.*

trait JacksonCodecInstancesBinCompat {
    protected class MacroTypeReference[A] extends TypeReference[A]

    implicit inline def jacksonFromJsonCodec[A](implicit inline om: ObjectMapper): Codec[JsonNode, A] =
        new Codec[JsonNode, A] {
            override def apply(a: JsonNode): Either[CodecFailure[JsonNode], A] = {
                try Right(om.readValue(om.treeAsTokens(a), new MacroTypeReference[A] {}))
                catch { case e: Exception => Left(new CodecFailure(e.getMessage, a, e)) }
            }
        }
}
