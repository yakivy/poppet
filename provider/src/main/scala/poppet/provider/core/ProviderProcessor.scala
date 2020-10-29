package poppet.provider.core

import scala.language.experimental.macros
import scala.reflect.macros.blackbox

class ProviderProcessor[I, F[_]](val service: String, val methods: List[MethodProcessor[I, F]])

class MethodProcessor[I, F[_]](
    val name: String, val arguments: List[String], val f: Map[String, I] => F[I]
)

object ProviderProcessor {
    def apply[S](service: S): PartialProviderProcessorApply[S] = new PartialProviderProcessorApply(service)
    class PartialProviderProcessorApply[S](service: S) {
        def generate[I, F[_]](): ProviderProcessor[I, F] = macro generateImpl[S, I, F]
    }

    def generateImpl[S, I, F[_]](
        c: blackbox.Context)()(
        implicit ST: c.WeakTypeTag[S], IT: c.WeakTypeTag[I], FT: c.WeakTypeTag[F[_]]
    ): c.universe.Tree = {
        import c.universe._
        val stype = ST.tpe
        val itype = IT.tpe
        val ftype = FT.tpe
        val service = c.prefix.tree.children.tail.head
        val fmonad = q"""implicitly[_root_.cats.Monad[$ftype]]"""
        val methodProcessors = stype.decls
            .filter(m => m.isAbstract)
            .map(_.asMethod)
            .map { m =>
                val argumentNames = m.paramLists.flatten.map(_.name.toString)
                val codedArgument: c.universe.Symbol => Tree = a => q"""implicitly[
                    _root_.poppet.all.ModelCoder[$itype,${appliedType(ftype, a.typeSignature)}]
                ].apply(as(${a.name.toString}))"""
                val withCodedArguments: Tree => Tree = tree => m.paramLists.flatten match {
                    case Nil => tree
                    case h :: Nil =>
                        q"""$fmonad.flatMap(${codedArgument(h)})((${Ident(h.name)}: ${h.typeSignature}) => $tree)"""
                    case hs => q"""$fmonad.flatten(
                        _root_.cats.Semigroupal.${TermName("map" + hs.size)}(..${hs.map(codedArgument)})(
                            ..${hs.map(h => q"${Ident(h.name)}: ${h.typeSignature}")} => $tree
                        )
                    )"""
                }
                val groupedArguments = m.paramLists.map(pl => pl.map(p => Ident(p.name)))
                q"""new _root_.poppet.provider.core.MethodProcessor[$itype, $ftype](
                    ${m.name.toString},
                    _root_.scala.List(..$argumentNames),
                    as => ${withCodedArguments(q"""
                        implicitly[_root_.poppet.all.ModelCoder[${m.returnType},${appliedType(ftype, itype)}]].apply(${
                        groupedArguments.foldLeft[Tree](
                            Select(service, m.name.toTermName))(
                            (acc, pl) => Apply(acc, pl)
                        )})
                    """)}
                )"""
            }.toList
        q"""new _root_.poppet.provider.ProviderProcessor[$itype,$ftype](
                ${stype.typeSymbol.fullName},
                $methodProcessors
        )"""
    }
}
