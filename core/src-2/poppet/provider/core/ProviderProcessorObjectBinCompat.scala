package poppet.provider.core

import poppet.core.ProcessorMacro
import scala.language.experimental.macros
import scala.reflect.macros.blackbox

trait ProviderProcessorObjectBinCompat {
    implicit def generate[F[_], I, S]: ProviderProcessor[F, I, S] =
        macro ProviderProcessorObjectBinCompat.generateImpl[F, I, S]
}

object ProviderProcessorObjectBinCompat {
    def generateImpl[F[_], I, S](
        c: blackbox.Context)(
        implicit FT: c.WeakTypeTag[F[_]], IT: c.WeakTypeTag[I], ST: c.WeakTypeTag[S]
    ): c.Expr[ProviderProcessor[F, I, S]] = {
        import c.universe._
        val fmonad = q"_root_.scala.Predef.implicitly[_root_.cats.Monad[$FT]]"
        val methodProcessors = ProcessorMacro.getAbstractMethods(c)(ST.tpe).map { m =>
            val mInS = m.typeSignatureIn(ST.tpe)
            val argumentNames = m.paramLists.flatten.map(_.name.toString)
            val (returnKind, returnType) = ProcessorMacro.separateReturnType(c)(FT.tpe, mInS.finalResultType, true)
            val codedArgument: c.universe.Symbol => Tree = a => q"""_root_.scala.Predef.implicitly[
                _root_.poppet.core.Codec[$IT,${ProcessorMacro.unwrapVararg(c)(a.typeSignature)}]
            ].apply(as(${a.name.toString})).fold($$fh.apply, $fmonad.pure)"""
            val withCodedArguments: Tree => Tree = tree => mInS.paramLists.flatten match {
                case Nil => tree
                case h :: Nil =>
                    q"$fmonad.flatMap(${codedArgument(h)})((${Ident(h.name)}: ${h.typeSignature}) => $tree)"
                case hs => q"""$fmonad.flatten(
                    $fmonad.${TermName("map" + hs.size)}(..${hs.map(codedArgument)})(
                        ..${hs.map(h => q"${Ident(h.name)}: ${h.typeSignature}")} => $tree
                    )
                )"""
            }
            //don't use returnTypeCodec, in some cases typecheck corrupts macro-based implicits
            val (returnKindCodec, returnTypeCodec) = ProcessorMacro.inferReturnCodecs(c)(
                returnKind, returnType, mInS.finalResultType,
                FT.tpe, IT.tpe, appliedType(FT.tpe, IT.tpe),
            )
            val groupedArguments = m.paramLists.map(pl => pl.map(p => p.typeSignature.typeSymbol -> Ident(p.name)))
            q"""new _root_.poppet.provider.core.MethodProcessor[$FT, $IT](
                ${ST.tpe.typeSymbol.fullName},
                ${m.name.toString},
                _root_.scala.List(..$argumentNames),
                as => ${withCodedArguments(q"""
                    $fmonad.flatMap(
                        $returnKindCodec.apply(${
                            groupedArguments.foldLeft[Tree](q"$$service.${m.name.toTermName}") { (acc, pl) =>
                                pl.lastOption match {
                                    case Some((s, i)) if s == definitions.RepeatedParamClass =>
                                        q"$acc(..${pl.init.map(_._2)}, $i: _*)"
                                    case _ => q"$acc(..${pl.map(_._2)})"
                                }
                            }
                        })
                    )(
                        _root_.scala.Predef.implicitly[_root_.poppet.core.Codec[$returnType,${IT.tpe}]]
                            .apply(_).fold($$fh.apply, $fmonad.pure)
                    )
                """)}
            )"""
        }
        c.Expr(q"(($$service, $$fh) => $methodProcessors)")
    }
}
