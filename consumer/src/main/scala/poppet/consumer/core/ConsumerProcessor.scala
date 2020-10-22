package poppet.consumer.core

import poppet._
import scala.language.experimental.macros
import scala.reflect.macros.blackbox

trait ConsumerProcessor[I, F[_], S] {
    def process(client: Request[I] => F[Response[I]]): S
}

object ConsumerProcessor {
    def apply[S]: PartialConsumerProcessorApply[S] = new PartialConsumerProcessorApply[S]
    class PartialConsumerProcessorApply[S] {
        def generate[I, F[_]](): ConsumerProcessor[I, F, S] = macro generateImpl[I, F, S]
    }

    def generateImpl[I, F[_], S](
        c: blackbox.Context)()(
        implicit IT: c.WeakTypeTag[I], FT: c.WeakTypeTag[F[_]], ST: c.WeakTypeTag[S]
    ): c.universe.Tree = {
        import c.universe._
        val stype = ST.tpe
        val itype = IT.tpe
        val ftype = FT.tpe
        val serviceName = stype.typeSymbol.fullName
        val fmonad = q"implicitly[_root_.cats.Monad[$ftype]]"
        val methods = stype.decls
            .filter(m => m.isAbstract)
            .map(_.asMethod)
            .map { m =>
                val methodName = m.name
                val arguments = m.paramLists.map(ps => ps.map(p => q"${Ident(p.name)}: ${p.typeSignature}"))
                val codedArgument: c.universe.Symbol => Tree = a => q"""implicitly[
                    _root_.poppet.Coder[${a.typeSignature},${appliedType(ftype, itype)}]
                ].apply(${Ident(a.name)})"""
                val withCodedArguments: Tree => Tree = tree => m.paramLists.flatten match {
                    case Nil => tree
                    case h :: Nil =>
                        q"""$fmonad.flatMap(${codedArgument(h)})((${Ident(h.name)}: ${h.typeSignature}) => $tree)"""
                    case hs => q"""$fmonad.flatten(
                        _root_.cats.Semigroupal.${TermName("map" + hs.size)}(..${hs.map(codedArgument)})(
                            ..${hs.map(h => q"${Ident(h.name)}: $itype")} => $tree
                        )
                    )"""
                }
                q"""override def $methodName(...$arguments): ${m.returnType} = {
                    val result = $fmonad.map(${withCodedArguments(q"""
                    client.apply(_root_.poppet.Request(
                        $serviceName, ${methodName.toString}, _root_.scala.Predef.Map(
                            ..${m.paramLists.flatten.map(p => q"""(
                                ${p.name.toString}, ${Ident(p.name)}
                            )""")}
                        )
                    ))""")})(_.value)
                    implicitly[_root_.poppet.Coder[${appliedType(ftype, itype)}, ${m.returnType}]].apply(result)
                }"""
            }.toList
        q"""(
           client => new $stype { ..$methods }
        ): _root_.poppet.consumer.ConsumerProcessor[$itype, $ftype, $stype]"""
    }
}
