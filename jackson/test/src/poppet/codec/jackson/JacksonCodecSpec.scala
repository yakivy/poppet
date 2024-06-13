package poppet.codec.jackson

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.ClassTagExtensions
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import org.scalatest.freespec.AnyFreeSpec
import poppet.codec.jackson.all._
import poppet.codec.CodecSpec
import poppet.codec.CodecSpec.A

class JacksonCodecSpec extends AnyFreeSpec with CodecSpec {

    implicit val objectMapper: ObjectMapper = {
        val objectMapper = new ObjectMapper() with ClassTagExtensions
        objectMapper.registerModule(DefaultScalaModule)
        objectMapper
    }

    "Jackson codec should parse" - {
        "custom data structures" in {
            assertCustomCodec[JsonNode, Unit](())
            assertCustomCodec[JsonNode, Int](intExample)
            assertCustomCodec[JsonNode, Long](1L)
            assertCustomCodec[JsonNode, String](stringExample)
            assertCustomCodec[JsonNode, A](caseClassExample)
        }
    }

}
