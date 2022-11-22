package poppet.codec.jackson.instances

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import poppet._
import poppet.codec.jackson.instances.JacksonCodecInstancesBinCompat._
import scala.language.experimental.macros
import scala.reflect.macros.blackbox

trait JacksonCodecInstancesBinCompat {
    implicit def jacksonFromJsonCodec[A](implicit om: ObjectMapper): Codec[JsonNode, A] =
        macro jacksonFromJsonCodecImpl[A]
}

object JacksonCodecInstancesBinCompat {
    def jacksonFromJsonCodecImpl[A](c: blackbox.Context)(om: c.Expr[ObjectMapper])(
        implicit AT: c.WeakTypeTag[A]
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
