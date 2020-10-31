package poppet.coder.jackson.instances

import cats.Applicative
import cats.Functor
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

    implicit def fromBytesExchangeCoder[A, F[_]](
        implicit mc: ModelCoder[JsonNode, F[A]]
    ): ExchangeCoder[Array[Byte], F[A]] = a => mc(om.readTree(a))
    implicit def toBytesExchangeCoder[A, F[_] : Functor](
        implicit mc: ModelCoder[A, F[JsonNode]]
    ): ExchangeCoder[A, F[Array[Byte]]] = a => mc(a).map(b => om.writeValueAsBytes(b))

    implicit def toJsonModelCoder[A, F[_] : Applicative](
        implicit mc: ModelCoder[JsonNode, A]
    ): ModelCoder[JsonNode, F[A]] = a => Applicative[F].pure(mc(a))
    implicit def fromJsonModelCoderF[A, F[_] : Applicative](
        implicit om: ObjectMapper
    ): ModelCoder[A, F[JsonNode]] = a => Applicative[F].pure(om.valueToTree(a))

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
