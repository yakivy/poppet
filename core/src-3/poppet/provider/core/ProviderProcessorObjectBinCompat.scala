package poppet.provider.core

import cats.Monad
import cats.Traverse
import poppet.provider.all._
import poppet.core.ProcessorMacro.*
import scala.quoted.*
import scala.compiletime.*

trait ProviderProcessorObjectBinCompat {
    implicit inline def generate[F[_], I, S](implicit inline MF: Monad[F]): ProviderProcessor[F, I, S] =
        ${ ProviderProcessorObjectBinCompat.processorExpr('MF) }
}

object ProviderProcessorObjectBinCompat {
    def processorExpr[F[_]: Type, I: Type, S: Type](
        using q: Quotes)(MF: Expr[Monad[F]]
    ): Expr[ProviderProcessor[F, I, S]] = '{
        new ProviderProcessor[F, I, S] {
            override def apply(service: S, fh: FailureHandler[F]): List[MethodProcessor[F, I]] =
                ${ ProviderProcessorObjectBinCompat.methodProcessorsImpl[F, I, S]('service, 'fh, MF) }
        }
    }

    def methodProcessorsImpl[F[_] : Type, I : Type, S : Type](
        using q: Quotes)(service: Expr[S], fh: Expr[FailureHandler[F]], MF: Expr[Monad[F]]
    ): Expr[List[poppet.provider.core.MethodProcessor[F, I]]] = {
        import q.reflect._
        val serviceName = TypeRepr.of[S].show
        val methodProcessors = getAbstractMethods[S].map { m =>
            def decodeArg(arg: ValDef): Expr[Map[String, I] => F[Any]] = {
                unwrapVararg(resolveTypeMember(TypeRepr.of[S], arg.tpt.tpe)).asType match { case '[at] => '{ input =>
                    summonInline[Codec[I, at]]
                        .apply(input(${Literal(StringConstant(arg.name)).asExprOf[String]}))
                        .fold($fh.apply, $MF.pure)
                }}
            }
            val decodeArgs: Expr[Map[String, I] => F[List[Any]]] = '{ input =>
                Traverse[List].sequence(
                    ${Expr.ofList(m.termParamss.flatMap(_.params).map(decodeArg))}.map(_(input))
                )(using $MF)
            }
            val (returnKind, returnType) = separateReturnType(
                TypeRepr.of[F], resolveTypeMember(TypeRepr.of[S], m.returnTpt.tpe), true
            )
            val (returnKindCodec, returnTypeCodec) = inferReturnCodecs(
                returnKind, returnType, m.returnTpt.tpe,
                TypeRepr.of[F], TypeRepr.of[I], TypeRepr.of[F[I]],
            )
            val callService: Expr[Map[String, I] => F[I]] = returnType.asType match { case '[rt] =>
                val paramTypes = m.termParamss.map(_.params.map(arg =>
                    arg -> resolveTypeMember(TypeRepr.of[S], arg.tpt.tpe)
                ))
                '{ input =>
                    $MF.flatMap(
                        $decodeArgs(input))(ast => $MF.flatMap(
                            ${Apply(TypeApply(
                                Select.unique(returnKindCodec, "apply"),
                                List(TypeTree.of[rt])),
                                List(paramTypes.foldLeft[(Term, Int)](
                                    Select(service.asTerm, m.symbol) -> 0)((acc, item) => (
                                        Apply(acc._1, item.zipWithIndex.map{ case ((arg, t), i) =>
                                            t.asType match { case '[at] =>
                                                val term = '{ast.apply(${Literal(IntConstant(i + acc._2)).asExprOf[Int]}).asInstanceOf[at]}.asTerm
                                                arg.tpt.tpe match {
                                                    case AnnotatedType(tpeP, t) if t.tpe.typeSymbol == defn.RepeatedAnnot =>
                                                        Typed(term, Inferred(defn.RepeatedParamClass.typeRef.appliedTo(tpeP.typeArgs)))
                                                    case _ => term
                                                }
                                            }
                                        }),
                                        (item.size + acc._2)
                                    )
                                )._1)
                            ).asExprOf[F[rt]]})(
                            ${returnTypeCodec.asExprOf[Codec[rt, I]]}.apply(_).fold($fh.apply, $MF.pure)
                        )
                    )
                }
            }
            '{MethodProcessor[F, I](
                ${Literal(StringConstant(serviceName)).asExprOf[String]},
                ${Literal(StringConstant(m.name)).asExprOf[String]},
                ${Expr.ofList(m.paramss.flatMap(_.params).map(n => Literal(StringConstant(n.name)).asExprOf[String]))},
                input => $callService(input)
            )}
        }.toList
        Expr.ofList(methodProcessors)
    }
}