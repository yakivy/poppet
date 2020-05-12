package poppet.provider

import cats.Monad
import scala.language.experimental.macros
import scala.reflect.macros.blackbox

class ProviderProcessor[I, F[_]](val service: String, val methods: List[MethodProcessor[I, F]])

class MethodProcessor[I, F[_]](
    val name: String, val arguments: Set[String], val f: Map[String, I] => F[I]
)

object ProviderProcessor {
    def generate[S]: PartialProviderProcessorApply[S] = new PartialProviderProcessorApply[S]
    class PartialProviderProcessorApply[S] {
        def apply[I, F[_]](
            service: S)(implicit M: Monad[F]
        ): ProviderProcessor[I, F] = macro generateImpl[S, I, F]
    }

    def generateImpl[S, I, F[_]](
        c: blackbox.Context)(service: c.Expr[S])(M: c.Expr[Monad[F]])(
        implicit ST: c.WeakTypeTag[S], IT: c.WeakTypeTag[I], FT: c.WeakTypeTag[F[_]]
    ): c.universe.Tree = {
        import c.universe._
        val itype = weakTypeTag[I].tpe
        val ftype = weakTypeTag[F[_]].tpe
        ftype.takesTypeArgs
        val serviceType = weakTypeTag[S].tpe
        val methodParsers = serviceType.decls
            .filter(m => m.isAbstract)
            .map(_.asMethod)
            .map { m =>
                val argumentNames = m.paramLists.flatten.map(_.name.toString)
                val codedArgument: c.universe.Symbol => Tree = c => q"""implicitly[
                    _root_.poppet.coder.Coder[$itype,$ftype[${c.typeSignature}]]
                ].apply(as(${c.name.toString}))"""
                val withCodedArguments: Tree => Tree = tree => m.paramLists.flatten match {
                    case Nil => tree
                    case h :: Nil =>
                        q"""$M.flatMap(${codedArgument(h)})((${Ident(h.name)}: ${h.typeSignature}) => $tree)"""
                    case hs => q"""$M.flatMap(
                        _root_.cats.Semigroupal.${TermName("tuple" + hs.size)}(..${hs.map(codedArgument)})){
                        case (..${hs.map(h => Ident(h.name))}) => $tree
                    }"""
                }
                val groupedArguments = m.paramLists.map(pl => pl.map(p => Ident(p.name)))
                q"""new _root_.poppet.provider.MethodProcessor[$itype, $ftype](
                    ${m.name.toString},
                    _root_.scala.Predef.Set(..$argumentNames),
                    as => ${
                    withCodedArguments(q"""
                        implicitly[_root_.poppet.coder.Coder[${m.returnType},$ftype[$itype]]].apply(${
                        groupedArguments.foldLeft[Tree](
                            Select(service.tree, m.name.toTermName))(
                            (acc, pl) => Apply(acc, pl)
                        )
                    })
                    """)
                }
                )"""
            }
        q"""
            println(
              ${methodParsers.toString()}
            )
            new _root_.poppet.provider.ProviderProcessor[$itype,$ftype](
                ${serviceType.typeSymbol.fullName},
                _root_.scala.List()
            )
        """
    }
}
