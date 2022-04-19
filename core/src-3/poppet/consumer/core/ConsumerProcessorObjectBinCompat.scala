package poppet.consumer.core

import cats.Monad
import poppet.consumer.all._
import poppet.core.ProcessorMacro
import poppet.core.Request
import poppet.core.Response
import scala.quoted._

trait ConsumerProcessorObjectBinCompat {
    implicit inline def apply[F[_], I, S](implicit inline MF: Monad[F]): ConsumerProcessor[F, I, S] = ${
        ConsumerProcessorObjectBinCompat.applyImpl[F, I, S]('MF)
    }
}

object ConsumerProcessorObjectBinCompat {
    def applyImpl[F[_]: Type, I: Type, S: Type](
        using q: Quotes)(MF: Expr[Monad[F]]
    ): Expr[ConsumerProcessor[F, I, S]] = {
        import q.reflect._
        val serviceName = TypeRepr.of[S].typeSymbol.fullName
        val implementations = (
            client: Expr[Request[I] => F[Response[I]]], fh: Expr[FailureHandler[F]]
        ) => ProcessorMacro.getAbstractMethods[S].map { m =>
            val codedArgument: ValDef => Expr[F[I]] = a => a.tpt.tpe.asType match { case '[at] => '{
                ${ProcessorMacro.inferImplicit[Codec[at,I]]}
                    .apply(${Ref(a.symbol).asExprOf[at]}).fold($fh.apply, $MF.pure)
            }}
            val codedArguments: List[ValDef] => Expr[F[Map[String, I]]] = _.foldLeft(
                '{$MF.pure(Map.empty[String, I])})(
                (acc, item) => '{$MF.flatMap($acc)(accR =>
                    $MF.map(${codedArgument(item)})(carg =>
                        accR + (${Literal(StringConstant(item.name)).asExprOf[String]} -> carg)
                    )
                )}
            )
            val (returnKind, returnType) = ProcessorMacro.separateReturnType(TypeRepr.of[F], m.returnTpt.tpe, false)
            val (returnKindCodec, returnTypeCodec) = ProcessorMacro.inferReturnCodecs(
                TypeRepr.of[F], TypeRepr.of[I], TypeRepr.of[F[I]],
                returnKind, returnType, m.returnTpt.tpe,
            )
            (returnType.asType) match { case '[rt] =>
                DefDef.copy(m)(m.name, m.paramss, m.returnTpt, Option(Apply(TypeApply(
                    Select.unique(returnKindCodec, "apply"),
                    List(TypeTree.of[rt])),
                    List('{
                        $MF.flatMap(
                            $MF.flatMap($MF.map(
                                ${codedArguments(m.termParamss.flatMap(_.params))})(args =>
                                Request[I](
                                    ${Literal(StringConstant(serviceName)).asExprOf[String]},
                                    ${Literal(StringConstant(m.name)).asExprOf[String]},
                                    args
                                )
                            ))($client.apply)
                        )(a => ${returnTypeCodec.asExprOf[Codec[I, rt]]}.apply(a.value).fold($fh.apply, $MF.pure))
                    }.asTerm),
                )))
            }
        }.toList
        val sterm = (
            client: Expr[Request[I] => F[Response[I]]], fh: Expr[FailureHandler[F]]
        ) => {
            val Inlined(_, _, Block((c: ClassDef) :: _, _)) = '{class Template}.asTerm
            val classDef = ClassDef.copy(c)(c.name, c.constructor, List(TypeTree.of[S]), None, implementations(client, fh))
            Block(
                List(classDef),
                Apply(Select.unique(New(TypeIdent(classDef.symbol)), "<init>"), Nil)
            ).asExpr
        }
        '{(client, fh) => ${
            sterm('client, 'fh)
            '{???}
        }}
    }
}
