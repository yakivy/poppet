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
import poppet.core._
import poppet.coder.instances.CoderInstances
import poppet.coder.jackson.instances.JacksonCoderInstances._
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

    implicit def toJsonCoder[A, F[_] : Applicative](
        implicit om: ObjectMapper
    ): Coder[A, F[JsonNode]] = a => Applicative[F].pure(om.valueToTree(a))

    implicit def fromJsonCoder[A](implicit om: ObjectMapper): Coder[JsonNode, A] =
        macro jsonToAnyCoderImpl[A]
}

object JacksonCoderInstances {
    def jsonToAnyCoderImpl[A](
        c: blackbox.Context)(om: c.Expr[com.fasterxml.jackson.databind.ObjectMapper])(implicit AT: c.WeakTypeTag[A]
    ): c.universe.Tree = {
        import c.universe._
        q"""new _root_.poppet.core.Coder[_root_.com.fasterxml.jackson.databind.JsonNode, $AT] {
            def apply(a: _root_.com.fasterxml.jackson.databind.JsonNode): $AT = $om.readValue(
                $om.treeAsTokens(a),
                new _root_.com.fasterxml.jackson.core.`type`.TypeReference[$AT] {}
            )
        }"""
    }
}
