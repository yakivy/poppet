package poppet.codec.jackson.instances

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.ClassTagExtensions
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import poppet._

trait JacksonCodenInstancesLp0 extends JacksonCodecInstancesBinCompat {
    implicit val om: ObjectMapper = {
        val om = new ObjectMapper() with ClassTagExtensions
        om.registerModule(DefaultScalaModule)
        om
    }

    implicit def jacksonToJsonCodec[A](implicit om: ObjectMapper): Codec[A, JsonNode] = a => {
        try Right(om.valueToTree(a))
        catch { case e: Exception => Left(new CodecFailure(e.getMessage, a, e)) }
    }
}

trait JacksonCodecInstances extends JacksonCodenInstancesLp0 {
    implicit def jacksonUnitToJsonCodec(implicit om: ObjectMapper): Codec[Unit, JsonNode] = _ =>
        Right(om.createObjectNode())
    implicit def jacksonJsonToUnitCodec(implicit om: ObjectMapper): Codec[JsonNode, Unit] = _ =>
        Right(())
}
