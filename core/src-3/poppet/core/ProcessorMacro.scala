package poppet.core

import scala.quoted._

object ProcessorMacro {
    def getAbstractMethods[S: Type](using q: Quotes): List[q.reflect.DefDef] = {
        import q.reflect._
        val methods = TypeRepr.of[S].typeSymbol.memberMethods.filter(s => {
            s.flags.is(Flags.Method) && (s.flags.is(Flags.Deferred) || s.flags.is(Flags.Abstract))
        }).sortBy(_.fullName).map(_.tree).collect {
            case m : DefDef if m.paramss.size == m.termParamss.size => m
            case m : DefDef => report.throwError(
                s"Generic methods are not supported: ${TypeRepr.of[S].typeSymbol.fullName}.${m.name}"
            )
        }
        if (methods.isEmpty) report.throwError(
            s"${TypeRepr.of[S].show} has no abstract methods. Make sure that service method is parametrized with trait."
        )
        if (methods.map(m => (m.name, m.paramss.flatMap(_.params).map(_.name.toString))).toSet.size != methods.size)
            report.throwError("Use unique argument name lists for overloaded methods.")
        methods
    }

    def separateReturnType(
        using q: Quotes)(fType: q.reflect.TypeRepr, returnType: q.reflect.TypeRepr
    ): (q.reflect.TypeRepr, q.reflect.TypeRepr) = {
        import q.reflect._
        (returnType match {
            case AppliedType(tycon, List(arg)) =>
                TypeRepr.of[CodecK].appliedTo(List(tycon, fType)).asType match { case '[ct] =>
                    Expr.summon[ct].map(_ => tycon -> arg)
                }
            case _ => None
        }).getOrElse(TypeRepr.of[cats.Id] -> returnType)
    }

    def inferImplicit[A: Type](using q: Quotes): Expr[A] = {
        import q.reflect._
        Implicits.search(TypeRepr.of[A]) match {
            case iss: ImplicitSearchSuccess => iss.tree.asExpr.asInstanceOf[Expr[A]]
            case isf: ImplicitSearchFailure => report.throwError(isf.explanation)
        }
    }

    def inferReturnCodecs(
        using q: Quotes)(
        fType: q.reflect.TypeRepr, faType: q.reflect.TypeRepr,
        tType: q.reflect.TypeRepr, taType: q.reflect.TypeRepr
    ): (q.reflect.Term, q.reflect.Term) = {
        import q.reflect._
        (
            TypeRepr.of[CodecK].appliedTo(List(fType, tType)).asType,
            TypeRepr.of[Codec].appliedTo(List(faType, taType)).asType,
            fType.appliedTo(faType).asType, tType.appliedTo(taType).asType,
        ) match { case ('[ckt], '[ct], '[fa], '[ta]) =>
            val codecK = Expr.summon[ckt]
            val codec = Expr.summon[ct]
            if (codecK.nonEmpty && codec.nonEmpty) (codecK.get.asTerm, codec.get.asTerm)
            else report.throwError(
                s"Unable to convert ${TypeRepr.of[fa]} to ${TypeRepr.of[ta]}. Try to provide " +
                s"${if (codecK.isEmpty) TypeRepr.of[ckt] else ""}" +
                (if (codecK.isEmpty && codec.isEmpty) " and " else "") +
                s"${if (codec.isEmpty) TypeRepr.of[ct] else "" }"
            )
        }

    }
}
