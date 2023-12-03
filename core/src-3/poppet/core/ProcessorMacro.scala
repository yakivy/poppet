package poppet.core

import scala.quoted.*
import poppet.*

object ProcessorMacro {
    def getAbstractMethods[S: Type](using q: Quotes): List[q.reflect.DefDef] = {
        import q.reflect._
        TypeRepr.of[S].typeSymbol.typeMembers.view.filter(_.flags.is(Flags.Deferred)).foreach { t =>
            report.throwError(s"Abstract types are not supported: ${TypeRepr.of[S].show}.${t.name}")
        }
        val methods = TypeRepr.of[S].typeSymbol.memberMethods.filter(s => {
            s.flags.is(Flags.Method) && (s.flags.is(Flags.Deferred) || s.flags.is(Flags.Abstract))
        }).sortBy(_.fullName).map(_.tree).collect {
            case m : DefDef if m.paramss.size == m.termParamss.size => m
            case m : DefDef => report.throwError(
                s"Generic methods are not supported: ${TypeRepr.of[S].show}.${m.name}"
            )
        }
        if (methods.isEmpty) report.throwError(
            s"${TypeRepr.of[S].show} has no abstract methods. Make sure that service method is parametrized with a trait."
        )
        if (methods.map(m => (m.name, m.paramss.flatMap(_.params).map(_.name.toString))).toSet.size != methods.size)
            report.throwError("Use unique argument name lists for overloaded methods.")
        methods
    }

    def unwrapVararg(using q: Quotes)(tpe: q.reflect.TypeRepr) = {
        import q.reflect._
        tpe match {
            case AnnotatedType(tpeP, t) if t.tpe.typeSymbol == defn.RepeatedAnnot =>
                TypeRepr.of[Seq].appliedTo(tpeP.typeArgs)
            case tpe if tpe.typeSymbol == defn.RepeatedParamClass =>
                TypeRepr.of[Seq].appliedTo(tpe.typeArgs)
            case _ => tpe
        }
    }

    def resolveTypeMember(
        using q: Quotes)(
        owner: q.reflect.TypeRepr,
        member: q.reflect.TypeRepr,
    ): q.reflect.TypeRepr = {
        val declarationOwner = owner.baseType(member.typeSymbol.maybeOwner)
        member.substituteTypes(declarationOwner.typeSymbol.memberTypes, declarationOwner.typeArgs)
    }

    def separateReturnType(
        using q: Quotes)(fType: q.reflect.TypeRepr, returnType: q.reflect.TypeRepr, fromReturn: Boolean
    ): (q.reflect.TypeRepr, q.reflect.TypeRepr) = {
        import q.reflect._
        (returnType match {
            case AppliedType(tycon, List(arg)) => Option(tycon -> arg)
            case _ => None
        }).flatMap { case (tycon, arg) =>
            TypeRepr.of[CodecK].appliedTo(
                if (fromReturn) List(tycon, fType) else List(fType, tycon)
            ).asType match { case '[ct] =>
                Expr.summon[ct].map(_ => tycon -> arg)
            }
        }.getOrElse(TypeRepr.of[cats.Id] -> returnType)
    }

    def summonImplicit[A: Type](using q: Quotes): Option[Expr[A]] = {
        import q.reflect._
        Implicits.search(TypeRepr.of[A]) match {
            case iss: ImplicitSearchSuccess => Option(iss.tree.asExpr.asInstanceOf[Expr[A]])
            case isf: AmbiguousImplicits => report.throwError(isf.explanation)
            case isf: ImplicitSearchFailure => None
        }
    }

    def inferReturnCodecs(
        using q: Quotes)(
        fType: q.reflect.TypeRepr, faType: q.reflect.TypeRepr, ffaType: q.reflect.TypeRepr,
        tType: q.reflect.TypeRepr, taType: q.reflect.TypeRepr, ttaType: q.reflect.TypeRepr,
    ): (q.reflect.Term, q.reflect.Term) = {
        import q.reflect._
        (
            TypeRepr.of[CodecK].appliedTo(List(fType, tType)).asType,
            TypeRepr.of[Codec].appliedTo(List(faType, taType)).asType,
        ) match { case ('[ckt], '[ct]) =>
            val codecK = summonImplicit[ckt]
            val codec = summonImplicit[ct]
            if (codecK.nonEmpty && codec.nonEmpty) (codecK.get.asTerm, codec.get.asTerm)
            else {
                def typeConstructorAndArgs(t: q.reflect.TypeRepr) = t match {
                    case AppliedType(tycon, args) => Option(tycon -> args)
                    case _ => None
                }
                def showType(t: q.reflect.TypeRepr) = t match {
                    case t if t =:= TypeRepr.of[cats.Id] => "cats.Id"
                    case TypeLambda(_, _, AppliedType(hkt, _)) => hkt.show
                    case _ => t.show
                }
                val taTypeConstructorAndArgs = typeConstructorAndArgs(taType)
                val faTypeConstructorAndArgs = typeConstructorAndArgs(faType)
                val ttaTypeConstructorAndArgs = typeConstructorAndArgs(ttaType)
                val ffaTypeConstructorAndArgs = typeConstructorAndArgs(ffaType)
                report.throwError(
                    s"Unable to convert ${showType(ffaType)} to ${showType(ttaType)}. Try to provide " +
                    (if (codecK.isEmpty) s"poppet.CodecK[${showType(fType)},${showType(tType)}]" else "") +
                    (if (codecK.isEmpty && codec.isEmpty) " with " else "") +
                    (if (codec.isEmpty) s"poppet.Codec[${showType(faType)},${showType(taType)}]" else "") +
                    (if (
                        !ttaTypeConstructorAndArgs.exists(_._1 =:= TypeRepr.of[cats.Id]) &&
                        tType =:= TypeRepr.of[cats.Id] &&
                        taTypeConstructorAndArgs.exists(_._2.size == 1)
                    ) {
                        val stType = taType match {
                            case AppliedType(tycon, _) => Option(tycon)
                            case _ => None
                        }
                        val staType = taTypeConstructorAndArgs.get._2.head
                        val scodec = TypeRepr.of[Codec].appliedTo(List(faType, staType)).asType match {
                            case ('[t]) => Expr.summon[t]
                        }
                        s" or poppet.CodecK[${showType(fType)},${showType(stType.get)}]" +
                            (if (scodec.isEmpty) s" with poppet.Codec[${showType(faType)},${showType(staType)}]" else "")
                    } else "") +
                    (if (
                        !ffaTypeConstructorAndArgs.exists(_._1 =:= TypeRepr.of[cats.Id]) &&
                        fType =:= TypeRepr.of[cats.Id] &&
                        faTypeConstructorAndArgs.exists(_._2.size == 1)
                    ) {
                        val stType = faType match {
                            case AppliedType(tycon, _) => Option(tycon)
                            case _ => None
                        }
                        val staType = faTypeConstructorAndArgs.get._2.head
                        val scodec = TypeRepr.of[Codec].appliedTo(List(staType, taType)).asType match {
                            case ('[t]) => Expr.summon[t]
                        }
                        s" or poppet.CodecK[${showType(stType.get)},${showType(tType)}]" +
                            (if (scodec.isEmpty) s" with poppet.Codec[${showType(staType)},${showType(taType)}]" else "")
                    } else "") +
                    "."
                )
            }
        }

    }
}
