package poppet.consumer.core

import poppet.core._
import poppet.internal.Processor
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
    ): c.Expr[ConsumerProcessor[I, F, S]] = {
        import c.universe._
        val serviceName = ST.tpe.typeSymbol.fullName
        val fmonad = q"implicitly[_root_.cats.Monad[$FT]]"
        val methodsToConsume =  ST.tpe.members.filter(m => m.isAbstract).map(_.asMethod)
        Processor.checkSignatures(c)(ST.tpe, methodsToConsume)
        val implementations = methodsToConsume.map { m =>
            val mInS = m.typeSignatureIn(ST.tpe)
            val methodName = m.name
            val arguments = mInS.paramLists.map(ps => ps.map(p => q"${Ident(p.name)}: ${p.typeSignature}"))
            val codedArgument: c.universe.Symbol => Tree = a => q"""implicitly[
                _root_.poppet.core.Coder[${a.typeSignature},${appliedType(FT.tpe, IT.tpe)}]
            ].apply(${Ident(a.name)})"""
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
            q"""override def $methodName(...$arguments): ${mInS.finalResultType} = {
                val result = $fmonad.map(${withCodedArguments(q"""
                client.apply(_root_.poppet.core.Request(
                    $serviceName, ${methodName.toString}, _root_.scala.Predef.Map(
                        ..${m.paramLists.flatten.map(p => q"""(
                            ${p.name.toString}, ${Ident(p.name)}
                        )""")}
                    )
                ))""")})(_.value)
                implicitly[_root_.poppet.core.Coder[${appliedType(FT.tpe, IT.tpe)}, ${mInS.finalResultType}]].apply(result)
            }"""
        }.toList
        c.Expr(q"""(client => new $ST { ..$implementations })""")
    }
}
