package poppet.coder.jackson.instances

import cats.FlatMap
import cats.implicits._
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.fasterxml.jackson.module.scala.ScalaObjectMapper
import poppet.all._
import poppet.coder.jackson.instances.JacksonCoderInstances._
import poppet.instances.CoderInstances
import scala.language.experimental.macros
import scala.reflect.macros.blackbox

trait JacksonCoderInstances extends CoderInstances {
    implicit val om: ObjectMapper = {
        val om = new ObjectMapper() with ScalaObjectMapper
        om.registerModule(DefaultScalaModule)
        om.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        om.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false)
        om
    }

    implicit def fromBytesExchangeCoder[A, F[_] : FlatMap](
        implicit mc: ModelCoder[JsonNode, F[A]], eh: ErrorHandler[JsonNode, F[JsonNode]]
    ): ExchangeCoder[Array[Byte], F[A]] = a => eh(om.readTree(a)).flatMap(mc)
    implicit def toBytesExchangeCoder[A, F[_] : FlatMap](
        implicit mc: ModelCoder[A, F[JsonNode]], eh: ErrorHandler[Array[Byte], F[Array[Byte]]]
    ): ExchangeCoder[A, F[Array[Byte]]] = a => mc(a).flatMap(b => eh(om.writeValueAsBytes(b)))

    implicit def toJsonModelCoder[A, F[_]](
        implicit eh: ErrorHandler[A, F[A]], mc: ModelCoder[JsonNode, A]
    ): ModelCoder[JsonNode, F[A]] = a => eh(mc(a))
    implicit def fromJsonModelCoderF[A, F[_]](
        implicit eh: ErrorHandler[JsonNode, F[JsonNode]], om: ObjectMapper
    ): ModelCoder[A, F[JsonNode]] = a => eh(om.valueToTree(a))

    implicit def fromJsonModelCoder[A](implicit om: ObjectMapper): ModelCoder[JsonNode, A] =
    macro jsonToAnyCoderImpl[A]
}

object JacksonCoderInstances {
    def jsonToAnyCoderImpl[A](
        c: blackbox.Context)(om: c.Expr[com.fasterxml.jackson.databind.ObjectMapper])(implicit AT: c.WeakTypeTag[A]
    ): c.universe.Tree = {
        import c.universe._
        q"""new _root_.poppet.ModelCoder[_root_.com.fasterxml.jackson.databind.JsonNode, $AT] {
            def apply(a: _root_.com.fasterxml.jackson.databind.JsonNode): $AT = $om.readValue(
                $om.treeAsTokens(a),
                new _root_.com.fasterxml.jackson.core.`type`.TypeReference[$AT] {}
            )
        }"""
    }
}
