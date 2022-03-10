package poppet.consumer.core

import poppet.core.ProcessorMacro
import scala.language.experimental.macros
import scala.reflect.macros.blackbox

trait ConsumerProcessorObjectBinCompat {
    implicit def apply[F[_], I, S]: ConsumerProcessor[F, I, S] =
        macro ConsumerProcessorObjectBinCompat.applyImpl[F, I, S]
}

object ConsumerProcessorObjectBinCompat {
    def applyImpl[F[_], I, S](
        c: blackbox.Context)(
        implicit FT: c.WeakTypeTag[F[_]], IT: c.WeakTypeTag[I], ST: c.WeakTypeTag[S]
    ): c.Expr[ConsumerProcessor[F, I, S]] = {
        import c.universe._
        val serviceName = ST.tpe.typeSymbol.fullName
        val fmonad = q"_root_.scala.Predef.implicitly[_root_.cats.Monad[$FT]]"
        val implementations = ProcessorMacro.getAbstractMethods(c)(ST.tpe).map { m =>
            val mInS = m.typeSignatureIn(ST.tpe)
            val methodName = m.name
            val arguments = mInS.paramLists.map(ps => ps.map(p => q"${Ident(p.name)}: ${p.typeSignature}"))
            val (returnKind, returnType) = ProcessorMacro.separateReturnType(c)(FT.tpe, mInS.finalResultType, false)
            val codedArgument: c.universe.Symbol => Tree = a => q"""_root_.scala.Predef.implicitly[
                _root_.poppet.core.Codec[${a.typeSignature},${IT.tpe}]
            ].apply(${Ident(a.name)}).fold($$fh.apply, $fmonad.pure)"""
            val withCodedArguments: Tree => Tree = tree => mInS.paramLists.flatten match {
                case Nil => tree
                case h :: Nil =>
                    q"""$fmonad.flatMap(${codedArgument(h)})((${Ident(h.name)}: ${h.typeSignature}) => $tree)"""
                case hs => q"""$fmonad.flatten(
                    _root_.cats.Semigroupal.${TermName("map" + hs.size)}(..${hs.map(codedArgument)})(
                        ..${hs.map(h => q"${Ident(h.name)}: $IT")} => $tree
                    )
                )"""
            }
            //don't use returnTypeCodec, in some cases typecheck corrupts macro-based implicits
            val (returnKindCodec, returnTypeCodec) = ProcessorMacro.inferReturnCodecs(c)(
                FT.tpe, IT.tpe, appliedType(FT.tpe, IT.tpe),
                returnKind, returnType, mInS.finalResultType
            )
            q"""override def $methodName(...$arguments): ${mInS.finalResultType} = {
                val result = $fmonad.map(${withCodedArguments(q"""
                $$client.apply(_root_.poppet.core.Request(
                    $serviceName, ${methodName.toString}, _root_.scala.Predef.Map(
                        ..${m.paramLists.flatten.map(p => q"""(
                            ${p.name.toString}, ${Ident(p.name)}
                        )""")}
                    )
                ))""")})(_.value)
                $returnKindCodec.apply($fmonad.flatMap(result)(
                    _root_.scala.Predef.implicitly[
                        _root_.poppet.core.Codec[${IT.tpe},$returnType]
                    ].apply(_).fold($$fh.apply, $fmonad.pure)
                ))
            }"""
        }
        c.Expr(q"""(($$client, $$fh) => new $ST { ..$implementations })""")
    }
}
