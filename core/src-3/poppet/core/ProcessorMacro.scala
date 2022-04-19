package poppet.core

import scala.quoted._
import poppet._

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
        using q: Quotes)(fType: q.reflect.TypeRepr, returnType: q.reflect.TypeRepr, fromReturn: Boolean
    ): (q.reflect.TypeRepr, q.reflect.TypeRepr) = {
        import q.reflect._
        (returnType match {
            case AppliedType(tycon, List(arg)) =>
                TypeRepr.of[CodecK].appliedTo(
                    if (fromReturn) List(tycon, fType) else List(fType, tycon)
                ).asType match { case '[ct] =>
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
        fType: q.reflect.TypeRepr, faType: q.reflect.TypeRepr, ffaType: q.reflect.TypeRepr,
        tType: q.reflect.TypeRepr, taType: q.reflect.TypeRepr, ttaType: q.reflect.TypeRepr,
    ): (q.reflect.Term, q.reflect.Term) = {
        import q.reflect._
        (
            TypeRepr.of[CodecK].appliedTo(List(fType, tType)).asType,
            TypeRepr.of[Codec].appliedTo(List(faType, taType)).asType,
            fType.appliedTo(faType).asType, tType.appliedTo(taType).asType,
        ) match { case ('[ckt], '[ct], '[fa], '[ta]) =>
            val codecK = Expr.summon[ckt]
            val codec = Expr.summon[ct]
            val ttaTypeConstructorAndArgs = ttaType match {
                case AppliedType(tycon, args) => Option(tycon -> args)
                case _ => None
            }
            val ffaTypeConstructorAndArgs = ffaType match {
                case AppliedType(tycon, args) => Option(tycon -> args)
                case _ => None
            }
            if (codecK.nonEmpty && codec.nonEmpty) (codecK.get.asTerm, codec.get.asTerm)
            else report.throwError(
                s"Unable to convert ${ffaType.show} to ${ttaType.show}. Try to provide " +
                    (if (codecK.isEmpty) s"poppet.CodecK[${fType.show},${tType.show}]" else "") +
                    (if (codecK.isEmpty && codec.isEmpty) " with " else "") +
                    (if (codec.isEmpty) s"poppet.Codec[${faType.show},${taType.show}] opa: ${TypeRepr.of[ct].show}" else "") +
                    (if (
                        !ttaTypeConstructorAndArgs.exists(_._1 =:= TypeRepr.of[cats.Id]) &&
                        tType =:= TypeRepr.of[cats.Id] &&
                        taTypeArgs.exists(_.size == 1)
                    ) {
                        val stType = taType match {
                            case AppliedType(tycon, _) => Option(tycon)
                            case _ => None
                        }
                        val staType = taType.typeArgs.head
                        val scodec = TypeRepr.of[Codec].appliedTo(List(faType, staType)).asType match {
                            case ('[t]) => Expr.summon[t]
                        }
                        s" or poppet.CodecK[${fType.show},${stType.get.show}]" +
                            (if (scodec.isEmpty) s" with poppet.Codec[${faType.show},${staType.show}]" else "")
                    } else "") +
                    (if (
                        !ttaTypeConstructor.exists(_ =:= TypeRepr.of[cats.Id]) &&
                        fType =:= TypeRepr.of[cats.Id] &&
                        faType.typeArgs.size == 1
                    ) {
                        val stType = faType match {
                            case AppliedType(tycon, _) => Option(tycon)
                            case _ => None
                        }
                        val staType = faType.typeArgs.head
                        val scodec = TypeRepr.of[Codec].appliedTo(List(staType, taType)).asType match {
                            case ('[t]) => Expr.summon[t]
                        }
                        s" or poppet.CodecK[${stType.get.show},${tType.show}]" +
                            (if (scodec.isEmpty) s" with poppet.Codec[${staType.show},${taType.show}]" else "")
                    } else "") +
                    "."
            )
        }

    }
}
