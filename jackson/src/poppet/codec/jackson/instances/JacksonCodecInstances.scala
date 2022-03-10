package poppet.codec.jackson.instances

import cats.Applicative
import cats.Functor
import cats.implicits._
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.fasterxml.jackson.module.scala.ScalaObjectMapper
import poppet._
import poppet.codec.jackson.instances.JacksonCodecInstances._
import scala.language.experimental.macros
import scala.reflect.macros.blackbox

trait JacksonCodenInstancesLp0 {
    implicit val om: ObjectMapper = {
        val om = new ObjectMapper() with ScalaObjectMapper
        om.registerModule(DefaultScalaModule)
        om.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        om.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false)
        om
    }

    implicit def jacksonToJsonCodec[A](implicit om: ObjectMapper): Codec[A, JsonNode] = a => {
        try Right(om.valueToTree(a))
        catch { case e: Exception => Left(new CodecFailure(e.getMessage, a, e)) }
    }

    implicit def jacksonFromJsonCodec[A](implicit om: ObjectMapper): Codec[JsonNode, A] =
        macro jacksonFromJsonCodecImpl[A]
}

trait JacksonCodecInstances extends JacksonCodenInstancesLp0 {
    implicit def jacksonUnitToJsonCodec(implicit om: ObjectMapper): Codec[Unit, JsonNode] = _ =>
        Right(om.createObjectNode())
    implicit def jacksonJsonToUnitCodec(implicit om: ObjectMapper): Codec[JsonNode, Unit] = _ =>
        Right(())
}

object JacksonCodecInstances {
    def jacksonFromJsonCodecImpl[A](
        c: blackbox.Context)(om: c.Expr[ObjectMapper])(implicit AT: c.WeakTypeTag[A]
    ): c.universe.Tree = {
        import c.universe._
        q"""new _root_.poppet.Codec[_root_.com.fasterxml.jackson.databind.JsonNode, $AT] {
            def apply(
                a: _root_.com.fasterxml.jackson.databind.JsonNode
            ): _root_.scala.Either[_root_.poppet.CodecFailure[_root_.com.fasterxml.jackson.databind.JsonNode], $AT] = {
                try _root_.scala.Right($om.readValue(
                    $om.treeAsTokens(a),
                    new _root_.com.fasterxml.jackson.core.`type`.TypeReference[$AT] {}
                ))
                catch { case e: _root_.scala.Exception => _root_.scala.Left(
                    new _root_.poppet.CodecFailure(e.getMessage, a, e)
                ) }
            }
        }"""
    }
}
