package poppet.consumer.core

import poppet.core._
import scala.language.experimental.macros
import scala.reflect.macros.blackbox

trait ConsumerProcessor[I, F[_], S] {
    def apply(client: Request[I] => F[Response[I]]): S
}

object ConsumerProcessor {
    implicit def apply[I, F[_], S]: ConsumerProcessor[I, F, S] = macro applyImpl[I, F, S]

    def applyImpl[I, F[_], S](
        c: blackbox.Context)(
        implicit IT: c.WeakTypeTag[I], FT: c.WeakTypeTag[F[_]], ST: c.WeakTypeTag[S]
    ): c.universe.Tree = {
        import c.universe._
        val serviceName = ST.tpe.typeSymbol.fullName
        val fmonad = q"implicitly[_root_.cats.Monad[$FT]]"
        val methods = ST.tpe.decls
            .filter(m => m.isAbstract)
            .map(_.asMethod)
            .map { m =>
                val methodName = m.name
                val arguments = m.paramLists.map(ps => ps.map(p => q"${Ident(p.name)}: ${p.typeSignature}"))
                val codedArgument: c.universe.Symbol => Tree = a => q"""implicitly[
                    _root_.poppet.core.Coder[${a.typeSignature},${appliedType(FT.tpe, IT.tpe)}]
                ].apply(${Ident(a.name)})"""
                val withCodedArguments: Tree => Tree = tree => m.paramLists.flatten match {
                    case Nil => tree
                    case h :: Nil =>
                        q"""$fmonad.flatMap(${codedArgument(h)})((${Ident(h.name)}: ${h.typeSignature}) => $tree)"""
                    case hs => q"""$fmonad.flatten(
                        _root_.cats.Semigroupal.${TermName("map" + hs.size)}(..${hs.map(codedArgument)})(
                            ..${hs.map(h => q"${Ident(h.name)}: $IT")} => $tree
                        )
                    )"""
                }
                q"""override def $methodName(...$arguments): ${m.returnType} = {
                    val result = $fmonad.map(${withCodedArguments(q"""
                    client.apply(_root_.poppet.core.Request(
                        $serviceName, ${methodName.toString}, _root_.scala.Predef.Map(
                            ..${m.paramLists.flatten.map(p => q"""(
                                ${p.name.toString}, ${Ident(p.name)}
                            )""")}
                        )
                    ))""")})(_.value)
                    implicitly[_root_.poppet.core.Coder[${appliedType(FT.tpe, IT.tpe)}, ${m.returnType}]].apply(result)
                }"""
            }.toList
        q"""(
           client => new $ST { ..$methods }
        ): _root_.poppet.consumer.core.ConsumerProcessor[$IT, $FT, $ST]"""
    }
}
