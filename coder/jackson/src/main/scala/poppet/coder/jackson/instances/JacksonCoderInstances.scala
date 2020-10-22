package poppet.coder.jackson.instances

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.fasterxml.jackson.module.scala.ScalaObjectMapper
import poppet.Coder
import poppet.coder.jackson.instances.JacksonCoderInstances._
import poppet.instances.CoderInstances
import scala.language.experimental.macros
import scala.reflect.macros.blackbox

trait LpJacksonCoderInstances extends CoderInstances {
    implicit val om: ObjectMapper = {
        val om = new ObjectMapper() with ScalaObjectMapper
        om.registerModule(DefaultScalaModule)
        om.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        om.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false)
        om
    }

    implicit def anyToJsonCoder[A](implicit om: ObjectMapper): Coder[A, JsonNode] = a => om.valueToTree(a)
    implicit def jsonToAnyCoder[A](implicit om: ObjectMapper): Coder[JsonNode, A] = macro jsonToAnyCoderImpl[A]
}

trait JacksonCoderInstances extends LpJacksonCoderInstances

object JacksonCoderInstances {
    def jsonToAnyCoderImpl[A](
        c: blackbox.Context)(om: c.Expr[com.fasterxml.jackson.databind.ObjectMapper])(implicit AT: c.WeakTypeTag[A]
    ): c.universe.Tree = {
        import c.universe._
        q"""new _root_.poppet.Coder[_root_.com.fasterxml.jackson.databind.JsonNode, $AT] {
            def apply(a: _root_.com.fasterxml.jackson.databind.JsonNode): $AT = $om.readValue(
                $om.treeAsTokens(a),
                new _root_.com.fasterxml.jackson.core.`type`.TypeReference[$AT] {}
            )
        }"""
    }
}
