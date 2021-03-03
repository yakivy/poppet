package poppet.provider.core

import poppet.internal.Processor
import scala.language.experimental.macros
import scala.reflect.macros.blackbox

trait ProviderProcessor[I, F[_], S] {
    def apply(service: S): List[MethodProcessor[I, F]]
}

class MethodProcessor[I, F[_]](
    val service: String, val name: String, val arguments: List[String], val f: Map[String, I] => F[I]
)

object ProviderProcessor {
    implicit def apply[I, F[_], S]: ProviderProcessor[I, F, S] = macro applyImpl[I, F, S]

    def applyImpl[I, F[_], S](
        c: blackbox.Context)(
        implicit IT: c.WeakTypeTag[I], FT: c.WeakTypeTag[F[_]], ST: c.WeakTypeTag[S]
    ): c.Expr[ProviderProcessor[I, F, S]] = {
        import c.universe._
        val fmonad = q"implicitly[_root_.cats.Monad[$FT]]"
        val methodsToProvide: Iterable[c.universe.MethodSymbol] = ST.tpe.members
            .filter(m => m.isAbstract)
            .map(_.asMethod)
        Processor.checkSignatures(c)(ST.tpe, methodsToProvide)
        val methodProcessors = methodsToProvide.map { m =>
            val mInS = m.typeSignatureIn(ST.tpe)
            val argumentNames = m.paramLists.flatten.map(_.name.toString)
            val codedArgument: c.universe.Symbol => Tree = a => q"""implicitly[
                _root_.poppet.core.Coder[$IT,${appliedType(FT.tpe, a.typeSignature)}]
            ].apply(as(${a.name.toString}))"""
            val withCodedArguments: Tree => Tree = tree => mInS.paramLists.flatten match {
                case Nil => tree
                case h :: Nil =>
                    q"$fmonad.flatMap(${codedArgument(h)})((${Ident(h.name)}: ${h.typeSignature}) => $tree)"
                case hs => q"""$fmonad.flatten(
                    _root_.cats.Semigroupal.${TermName("map" + hs.size)}(..${hs.map(codedArgument)})(
                        ..${hs.map(h => q"${Ident(h.name)}: ${h.typeSignature}")} => $tree
                    )
                )"""
            }
            val groupedArguments = m.paramLists.map(pl => pl.map(p => Ident(p.name)))
            q"""new _root_.poppet.provider.core.MethodProcessor[$IT, $FT](
                ${ST.tpe.typeSymbol.fullName},
                ${m.name.toString},
                _root_.scala.List(..$argumentNames),
                as => ${withCodedArguments(q"""
                    implicitly[_root_.poppet.core.Coder[${mInS.finalResultType},${appliedType(FT.tpe, IT.tpe)}]].apply(${
                    groupedArguments.foldLeft[Tree](
                        q"service.${m.name.toTermName}")((acc, pl) => Apply(acc, pl)
                    )})
                """)}
            )"""
        }.toList
        c.Expr(q"(service => $methodProcessors)")
    }
}
