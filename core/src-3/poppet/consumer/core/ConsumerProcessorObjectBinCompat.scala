package poppet.consumer.core

import cats.Monad
import cats.Traverse
import java.util.UUID
import poppet.consumer.all.*
import poppet.core.ProcessorMacro.*
import poppet.core.Request
import poppet.core.Response
import scala.annotation.experimental
import scala.quoted.*
import scala.compiletime.*

trait ConsumerProcessorObjectBinCompat {
    implicit inline def apply[F[_], I, S](implicit inline MF: Monad[F]): ConsumerProcessor[F, I, S] =
        ${ ConsumerProcessorObjectBinCompat.processorExpr('MF) }
}

@experimental
object ConsumerProcessorObjectBinCompat {
    import scala.language.experimental.*

    def processorExpr[F[_]: Type, I: Type, S: Type](
        using q: Quotes)(MF: Expr[Monad[F]]
    ): Expr[ConsumerProcessor[F, I, S]] = '{
        new ConsumerProcessor[F, I, S] {
            override def apply(client: Request[I] => F[Response[I]], fh: FailureHandler[F]): S =
                ${ ConsumerProcessorObjectBinCompat.serviceImplExpr[F, I, S]('client, 'fh, MF) }
        }
    }

    private def serviceImplExpr[F[_]: Type, I: Type, S: Type](
        using q: Quotes)(client: Expr[Request[I] => F[Response[I]]], fh: Expr[FailureHandler[F]], MF: Expr[Monad[F]]
    ): Expr[S] = {
        import q.reflect._
        def methodSymbols(classSymbol: Symbol) = getAbstractMethods[S].map(m =>
            Symbol.newMethod(classSymbol, m.name, TypeRepr.of[S].memberType(m.symbol))
        )
        def methodImpls(classSymbol: Symbol) = classSymbol.declaredMethods.map(m =>
            DefDef(m, argss => Option(methodBodyTerm[F, I, S](m, argss, client, fh, MF).changeOwner(m)))
                .changeOwner(classSymbol)
        )
        val className = s"$$PoppetConsumer_${TypeRepr.of[S].show}_${UUID.randomUUID()}"
        val parents = List(TypeTree.of[Object], TypeTree.of[S])
        val classSymbol = Symbol.newClass(
            Symbol.spliceOwner, className, parents.map(_.tpe), methodSymbols, None
        )
        val classDef = ClassDef(classSymbol, parents, methodImpls(classSymbol))
        Block(
            List(classDef),
            Typed(Apply(Select(New(TypeIdent(classDef.symbol)), classSymbol.primaryConstructor), Nil), TypeTree.of[S])
        ).asExprOf
    }

    private def methodBodyTerm[F[_]: Type, I: Type, S: Type](
        using q: Quotes
    )(
        methodSymbol: q.reflect.Symbol,
        argss: List[List[q.reflect.Tree]],
        client: Expr[Request[I] => F[Response[I]]],
        fh: Expr[FailureHandler[F]],
        MF: Expr[Monad[F]]
    ): q.reflect.Term = {
        import q.reflect._
        val methodReturnTpe = methodSymbol.tree.asInstanceOf[DefDef].returnTpt.tpe
        def codedArgument(a: Tree): Expr[F[I]] = resolveTypeMember(
            TypeRepr.of[S], Ref(a.symbol).tpe.widen
        ).asType match { case '[at] =>
            '{ summonInline[Codec[at,I]].apply(${Ref.term(a.symbol.termRef).asExprOf[at]}).fold($fh.apply, $MF.pure) }
        }
        def codedArguments(terms: List[Tree]): Expr[F[Map[String, I]]] = {
            '{$MF.map(Traverse[List].sequence(${Expr.ofList(terms.map(t => '{
                $MF.map(${codedArgument(t)})(${Literal(StringConstant(t.symbol.name)).asExprOf[String]} -> _)
            }))})(using $MF))(_.toMap)}
        }
        val (returnKind, returnType) = separateReturnType(TypeRepr.of[F], methodReturnTpe, false)
        val (returnKindCodec, returnTypeCodec) = inferReturnCodecs(
            TypeRepr.of[F], TypeRepr.of[I], TypeRepr.of[F[I]],
            returnKind, returnType, methodReturnTpe,
        )
        (
            methodReturnTpe.asType,
            returnType.asType,
        ) match { case ('[rtt], '[rt]) =>
            Apply(TypeApply(Select.unique(returnKindCodec, "apply"), List(TypeTree.of[rt])), List('{
                $MF.flatMap(
                    $MF.flatMap($MF.map(
                        ${codedArguments(argss.flatten)})(args =>
                        Request[I](
                            ${Literal(StringConstant(TypeRepr.of[S].show)).asExprOf[String]},
                            ${Literal(StringConstant(methodSymbol.name)).asExprOf[String]},
                            args
                        )
                    ))(a => $client.apply(a))
                )(a => {${returnTypeCodec.asExprOf[Codec[I, rt]]}.apply((a: Response[I]).value)}.fold($fh.apply, $MF.pure))
            }.asTerm))
        }
    }
}
