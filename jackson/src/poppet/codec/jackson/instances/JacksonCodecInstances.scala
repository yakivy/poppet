package poppet.codec.jackson.instances

import cats.Applicative
import cats.Functor
import cats.implicits._
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.fasterxml.jackson.module.scala.ClassTagExtensions
import poppet._

trait JacksonCodenInstancesLp0 extends JacksonCodecInstancesBinCompat {
    implicit val om: ObjectMapper = {
        val om = new ObjectMapper() with ClassTagExtensions
        om.registerModule(DefaultScalaModule)
        om.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        om.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false)
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
