package poppet.provider.core

import cats.Monad
import poppet.provider.all._
import poppet.core.ProcessorMacro
import scala.quoted._

trait ProviderProcessorObjectBinCompat {
    implicit inline def apply[F[_], I, S](implicit inline MF: Monad[F]): ProviderProcessor[F, I, S] = ${
        ProviderProcessorObjectBinCompat.applyImpl[F, I, S]('MF)
    }
}

object ProviderProcessorObjectBinCompat {
    def applyImpl[F[_] : Type, I : Type, S : Type](
        using q: Quotes)(MF: Expr[Monad[F]]
    ): Expr[ProviderProcessor[F, I, S]] = {
        import q.reflect._
        val serviceName = TypeRepr.of[S].typeSymbol.fullName
        def methodProcessors(
            service: Expr[S], fh: Expr[FailureHandler[F]]
        ): List[Expr[MethodProcessor[F, I]]] = ProcessorMacro.getAbstractMethods[S].map { m =>
            def decodeArg(arg: ValDef, input: Expr[Map[String, I]]): Expr[F[Any]] = {
                arg.tpt.tpe.asType match { case '[at] => '{
                    ${ProcessorMacro.inferImplicit[Codec[I, at]]}
                        .apply($input(${Literal(StringConstant(arg.name)).asExprOf[String]}))
                        .fold($fh.apply, $MF.pure)
                }}
            }
            def decodeArgs(input: Expr[Map[String, I]]): Expr[F[List[Any]]] = {
                m.termParamss.flatMap(_.params).reverse.foldLeft(
                    '{$MF.pure[List[Any]](List.empty)})(
                    (acc, item) => '{$MF.flatMap($acc)(accR =>
                        $MF.map(${decodeArg(item, input).asExprOf[F[Any]]})(ca => ca :: accR)
                    )}
                )
            }
            val (returnKind, returnType) = ProcessorMacro.separateReturnType(TypeRepr.of[F], m.returnTpt.tpe)
            val (returnKindCodec, returnTypeCodec) = ProcessorMacro.inferReturnCodecs(
                returnKind, returnType, TypeRepr.of[F], TypeRepr.of[I]
            )
            returnType.asType match { case '[rt] =>
                def callService(input: Expr[Map[String, I]]): Expr[F[I]] = '{$MF.flatMap(
                    ${decodeArgs(input)})(ast => $MF.flatMap(
                        ${Apply(TypeApply(
                            Select.unique(returnKindCodec, "apply"),
                            List(TypeTree.of[rt])),
                            List(m.termParamss.map(_.params).foldLeft[(Term, Int)](
                                Select(service.asTerm, m.symbol) -> 0)((acc, item) =>
                                Apply(acc._1, item.zipWithIndex.map{ t => t._1.tpt.tpe.asType match { case '[at] =>
                                    '{ast.apply(${Literal(IntConstant(t._2 + acc._2)).asExprOf[Int]}).asInstanceOf[at]}.asTerm
                                }}) -> (item.size + acc._2)
                            )._1)
                        ).asExprOf[F[rt]]})(
                        ${returnTypeCodec.asExprOf[Codec[rt, I]]}.apply(_).fold($fh.apply, $MF.pure)
                    )
                )}
                '{MethodProcessor[F, I](
                    ${Literal(StringConstant(serviceName)).asExprOf[String]},
                    ${Literal(StringConstant(m.name)).asExprOf[String]},
                    ${Expr.ofList(m.paramss.flatMap(_.params).map(n => Literal(StringConstant(n.name)).asExprOf[String]))},
                    input => ${callService('input)}
                )}
            }
        }.toList
        '{(service, fh) => ${Expr.ofList(methodProcessors('service, 'fh))}}
    }
}