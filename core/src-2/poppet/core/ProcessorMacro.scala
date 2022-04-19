package poppet.core

import scala.reflect.macros.TypecheckException
import scala.reflect.macros.blackbox

object ProcessorMacro {
    def getAbstractMethods(c: blackbox.Context)(tpe: c.Type): List[c.universe.MethodSymbol] = {
        val methods = tpe.members.toList.filter(m => m.isAbstract).map(_.asMethod).sortBy(_.fullName)
        methods.foreach { m =>
            if (m.typeParams.nonEmpty) c.abort(c.enclosingPosition, s"Generic methods are not supported: $tpe.${m.name}")
        }
        if (methods.isEmpty) c.abort(c.enclosingPosition,
            s"$tpe has no abstract methods. Make sure that service method is parametrized with a trait."
        )
        if (methods.map(m => (m.name, m.paramLists.flatten.map(_.name.toString))).toSet.size != methods.size)
            c.abort(c.enclosingPosition, "Use unique argument name lists for overloaded methods.")
        methods
    }

    def inferImplicit[A: c.universe.Liftable](c: blackbox.Context)(tpe: A): Option[c.Tree] = {
        import c.universe._
        try c.typecheck(q"""_root_.scala.Predef.implicitly[$tpe]""") match {
            case EmptyTree => None
            case tree => Option(tree)
        }
        catch {
            case e: TypecheckException if e.msg.contains("could not find implicit value for") => None
            case e: TypecheckException => c.abort(c.enclosingPosition, e.msg)
        }
    }

    def separateReturnType(
        c: blackbox.Context)(fType: c.Type, returnType: c.Type, fromReturn: Boolean
    ): (c.Type, c.Type) = {
        import c.universe._
        val typeArgs = returnType.typeArgs
        (if (typeArgs.size == 1) {
            ProcessorMacro.inferImplicit(c)(
                if (fromReturn) tq"_root_.poppet.CodecK[${returnType.typeConstructor}, $fType]"
                else tq"_root_.poppet.CodecK[$fType,${returnType.typeConstructor}]"
            ).map(_ => returnType.typeConstructor -> typeArgs.last)
        } else None).getOrElse(typeOf[cats.Id[_]].typeConstructor -> returnType)
    }

    def inferReturnCodecs(
        c: blackbox.Context)(
        fType: c.Type, faType: c.Type, ffaType: c.Type,
        tType: c.Type, taType: c.Type, ttaType: c.Type,
    ): (c.Tree, c.Tree) = {
        import c.universe._
        val codecK = ProcessorMacro.inferImplicit(c)(tq"_root_.poppet.CodecK[$fType,$tType]")
        val codec = ProcessorMacro.inferImplicit(c)(tq"_root_.poppet.Codec[$faType,$taType]")
        if (codecK.nonEmpty && codec.nonEmpty) (codecK.get, codec.get)
        else c.abort(
            c.enclosingPosition,
            s"Unable to convert $ffaType to $ttaType. Try to provide " +
                (if (codecK.isEmpty) s"poppet.CodecK[$fType,$tType]" else "") +
                (if (codecK.isEmpty && codec.isEmpty) " with " else "") +
                (if (codec.isEmpty) s"poppet.Codec[$faType,$taType]" else "") +
                (if (
                    !(ttaType.typeConstructor =:= typeOf[cats.Id[_]].typeConstructor) &&
                    tType =:= typeOf[cats.Id[_]].typeConstructor &&
                    taType.typeArgs.size == 1
                ) {
                    val stType = taType.typeConstructor
                    val staType = taType.typeArgs.head
                    val scodec = ProcessorMacro.inferImplicit(c)(tq"_root_.poppet.Codec[$faType,$staType]")
                    s" or poppet.CodecK[$fType,$stType]" +
                        (if (scodec.isEmpty) s" with poppet.Codec[$faType,$staType]" else "")
                } else "") +
                (if (
                    !(ffaType.typeConstructor =:= typeOf[cats.Id[_]].typeConstructor) &&
                    fType =:= typeOf[cats.Id[_]].typeConstructor &&
                    faType.typeArgs.size == 1
                ) {
                    val stType = faType.typeConstructor
                    val staType = faType.typeArgs.head
                    val scodec = ProcessorMacro.inferImplicit(c)(tq"_root_.poppet.Codec[$staType,$taType]")
                    s" or poppet.CodecK[$stType,$tType]" +
                        (if (scodec.isEmpty) s" with poppet.Codec[$staType,$taType]" else "")
                } else "") +
                "."
        )
    }
}
