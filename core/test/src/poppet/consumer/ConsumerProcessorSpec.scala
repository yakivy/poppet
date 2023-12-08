package poppet.consumer

import cats._
import cats.data.EitherT
import org.scalatest.freespec.AsyncFreeSpec
import poppet.consumer.core.ConsumerProcessor
import poppet.core.ProcessorSpec
import poppet.core.ProcessorSpec._
import poppet.core.Request
import poppet.core.Response
import scala.concurrent.Future

class ConsumerProcessorSpec extends AsyncFreeSpec with ProcessorSpec {
    "Consumer processor" - {
        "should generate instance" - {
            implicit val c0: Codec[String, Int] = a => Right(a.toInt)
            implicit val c1: Codec[String, List[Int]] = a => Right(List(a.toInt))
            implicit val c2: Codec[String, SimpleDto] = a => Right(SimpleDto(a.toInt))
            implicit val c3: Codec[String, List[String]] = a => Right(List(a))
            implicit val cp0: Codec[Boolean, String] = a => Right(a.toString)
            implicit val cp1: Codec[Option[Boolean], String] = a => Right(a.getOrElse(false).toString)
            implicit val cp2: Codec[Seq[Boolean], String] = a => Right(a.mkString(","))
            var request: Request[String] = null

            "when has id data kind" - {
                val client: Request[String] => Response[String] = r => {
                    request = r
                    Response("0")
                }
                "for methods with different arguments number" in {
                    val a = ConsumerProcessor.generate[Id, String, Simple].apply(client, FailureHandler.throwing)

                    assert(a.a0 == 0 && request == Request[String](
                        "poppet.core.ProcessorSpec.Simple",
                        "a0",
                        Map.empty
                    ))
                    assert(a.a00() == List(0) && request == Request[String](
                        "poppet.core.ProcessorSpec.Simple",
                        "a00",
                        Map.empty
                    ))
                    assert(a.a1(true) == SimpleDto(0) && request == Request(
                        "poppet.core.ProcessorSpec.Simple",
                        "a1",
                        Map("b" -> "true")
                    ))
                    assert(a.a2(true, None) == List("0") && request == Request(
                        "poppet.core.ProcessorSpec.Simple",
                        "a2",
                        Map("b0" -> "true", "b1" -> "false")
                    ))
                }
                "for methods with future kind" in {
                    implicit val ck: CodecK[Id, Future] = new CodecK[Id, Future] {
                        override def apply[A](a: Id[A]): Future[A] = Future.successful(a)
                    }
                    val a = ConsumerProcessor[Id, String, WithFutureKind].apply(client, FailureHandler.throwing)

                    assert(a.a0.value.get.get == 0 && request == Request[String](
                        "poppet.core.ProcessorSpec.WithFutureKind",
                        "a0",
                        Map.empty
                    ))
                    assert(a.a1.value.get.get == List(0) && request == Request[String](
                        "poppet.core.ProcessorSpec.WithFutureKind",
                        "a1",
                        Map.empty
                    ))
                }
                "for methods with multiple argument lists" in {
                    val a =
                        ConsumerProcessor[Id, String, WithMultipleArgumentLists].apply(client, FailureHandler.throwing)

                    assert(a.a0(true)(false) == 0 && request == Request(
                        "poppet.core.ProcessorSpec.WithMultipleArgumentLists",
                        "a0",
                        Map(
                            "b0" -> "true",
                            "b1" -> "false"
                        )
                    ))
                    assert(a.a1(true)()(false) == List(0) && request == Request(
                        "poppet.core.ProcessorSpec.WithMultipleArgumentLists",
                        "a1",
                        Map(
                            "b0" -> "true",
                            "b1" -> "false"
                        )
                    ))
                    assert(a.a2(true)(false, true) == SimpleDto(0) && request == Request(
                        "poppet.core.ProcessorSpec.WithMultipleArgumentLists",
                        "a2",
                        Map(
                            "b0" -> "true",
                            "b10" -> "false",
                            "b11" -> "true"
                        )
                    ))
                }
                "for methods with default arguments" in {
                    val a: WithDefaultArguments =
                        ConsumerProcessor[Id, String, WithDefaultArguments].apply(client, FailureHandler.throwing)

                    assert(a.a0(false) == 0 && request == Request(
                        "poppet.core.ProcessorSpec.WithDefaultArguments",
                        "a0",
                        Map("b" -> "false")
                    ))
                    assert(a.a0() == 0 && request == Request(
                        "poppet.core.ProcessorSpec.WithDefaultArguments",
                        "a0",
                        Map("b" -> "true")
                    ))
                    assert(a.a1(false) == List(0) && request == Request(
                        "poppet.core.ProcessorSpec.WithDefaultArguments",
                        "a1",
                        Map("b0" -> "false", "b1" -> "true")
                    ))
                    assert(a.a1(true, false) == List(0) && request == Request(
                        "poppet.core.ProcessorSpec.WithDefaultArguments",
                        "a1",
                        Map("b0" -> "true", "b1" -> "false")
                    ))
                    assert(a.a2(false, false) == SimpleDto(0) && request == Request(
                        "poppet.core.ProcessorSpec.WithDefaultArguments",
                        "a2",
                        Map(
                            "b0" -> "false",
                            "b1" -> "false",
                            "b2" -> "true",
                            "b3" -> "true"
                        )
                    ))
                    assert(a.a2(true, true, false) == SimpleDto(0) && request == Request(
                        "poppet.core.ProcessorSpec.WithDefaultArguments",
                        "a2",
                        Map(
                            "b0" -> "true",
                            "b1" -> "true",
                            "b2" -> "false",
                            "b3" -> "true"
                        )
                    ))
                    assert(a.a2(true, true, false, false) == SimpleDto(0) && request == Request(
                        "poppet.core.ProcessorSpec.WithDefaultArguments",
                        "a2",
                        Map(
                            "b0" -> "true",
                            "b1" -> "true",
                            "b2" -> "false",
                            "b3" -> "false"
                        )
                    ))
                }
                "for traits with generic hierarchy" in {
                    val a = ConsumerProcessor[Id, String, WithParentWithParameters]
                        .apply(client, FailureHandler.throwing)

                    assert(a.a0(false) == 0 && request == Request(
                        "poppet.core.ProcessorSpec.WithParentWithParameters",
                        "a0",
                        Map("b0" -> "false")
                    ))
                    assert(a.a1 == 0 && request == Request[String](
                        "poppet.core.ProcessorSpec.WithParentWithParameters",
                        "a1",
                        Map.empty
                    ))
                }
                "for methods with varargs" in {
                    val a = ConsumerProcessor[Id, String, WithVarargs]
                        .apply(client, FailureHandler.throwing)

                    assert(a.a0(false, true) == 0 && request == Request(
                        "poppet.core.ProcessorSpec.WithVarargs",
                        "a0",
                        Map("b" -> "false,true")
                    ))
                }
            }
            "when has future data kind" in {
                import cats.implicits._
                import scala.concurrent.Future

                implicit val ck0: CodecK[Future, cats.Id] = new CodecK[Future, cats.Id] {
                    override def apply[A](a: Future[A]): Id[A] = a.value.get.get
                }

                val a = ConsumerProcessor[Future, String, Simple].apply(
                    r => {
                        request = r
                        Future.successful(Response("0"))
                    },
                    FailureHandler.throwing
                )

                assert(a.a0 == 0 && request == Request[String](
                    "poppet.core.ProcessorSpec.Simple",
                    "a0",
                    Map.empty
                ))
                assert(a.a00() == List(0) && request == Request[String](
                    "poppet.core.ProcessorSpec.Simple",
                    "a00",
                    Map.empty
                ))
                assert(a.a1(true) == SimpleDto(0) && request == Request(
                    "poppet.core.ProcessorSpec.Simple",
                    "a1",
                    Map("b" -> "true")
                ))
                assert(a.a2(true, Option(false)) == List("0") && request == Request(
                    "poppet.core.ProcessorSpec.Simple",
                    "a2",
                    Map("b0" -> "true", "b1" -> "false")
                ))
            }
            "when has complex data kind" in {
                import cats.implicits._

                implicit val ck: CodecK[WithComplexReturnTypes.ReturnType, cats.Id] =
                    new CodecK[WithComplexReturnTypes.ReturnType, cats.Id] {
                        override def apply[A](a: WithComplexReturnTypes.ReturnType[A]): Id[A] =
                            a.value.value.get.get.toOption.get
                    }

                val p = ConsumerProcessor[WithComplexReturnTypes.ReturnType, String, WithComplexReturnTypes].apply(
                    r => {
                        request = r
                        EitherT.pure(Response("0"))
                    },
                    FailureHandler.throwing
                )

                def result[A](value: WithComplexReturnTypes.ReturnType[A]): A =
                    value.value.value.get.get.toOption.get

                assert(result(p.a0(true)) == 0 && request == Request(
                    "poppet.core.ProcessorSpec.WithComplexReturnTypes",
                    "a0",
                    Map("b" -> "true")
                ))
                assert(result(p.a1(true, false)) == 0 && request == Request(
                    "poppet.core.ProcessorSpec.WithComplexReturnTypes",
                    "a1",
                    Map("b0" -> "true", "b1" -> "false")
                ))
            }
            "when has A data kind and service has B data kind" in {
                import cats.implicits._

                type F[A] = Option[A]
                type G[A] = WithComplexReturnTypes.ReturnType[A]

                implicit val ck0: CodecK[F, G] = new CodecK[F, G] {
                    override def apply[A](a: F[A]): G[A] = EitherT.fromEither(a.toRight("not found"))
                }
                implicit val ck1: CodecK[G, F] = new CodecK[G, F] {
                    override def apply[A](a: G[A]): F[A] = a.value.value.get.get.toOption
                }

                val p = ConsumerProcessor[F, String, WithComplexReturnTypes].apply(
                    r => {
                        request = r
                        Option(Response("0"))
                    },
                    FailureHandler.throwing
                )

                def result[A](value: WithComplexReturnTypes.ReturnType[A]): A =
                    value.value.value.get.get.toOption.get

                assert(result(p.a0(true)) == 0 && request == Request(
                    "poppet.core.ProcessorSpec.WithComplexReturnTypes",
                    "a0",
                    Map("b" -> "true")
                ))
                assert(result(p.a1(true, true)) == 0 && request == Request(
                    "poppet.core.ProcessorSpec.WithComplexReturnTypes",
                    "a1",
                    Map("b0" -> "true", "b1" -> "true")
                ))
            }
        }

        "shouldn't generate instance" - {
            "when has id data kind" - {
                "for trait with generic methods" in {
                    assertCompilationErrorMessage(
                        assertCompiles("""ConsumerProcessor[Id, String, WithMethodWithParameters]"""),
                        "Generic methods are not supported: " +
                            "poppet.core.ProcessorSpec.WithMethodWithParameters.a"
                    )
                }
                "for trait without abstract methods" in {
                    assertCompilationErrorMessage(
                        assertCompiles("""ConsumerProcessor[Id, String, WithNoAbstractMethods]"""),
                        "poppet.core.ProcessorSpec.WithNoAbstractMethods has no abstract methods. " +
                            "Make sure that service method is parametrized with a trait."
                    )
                }
                "for trait with conflicting methods" in {
                    assertCompilationErrorMessage(
                        assertCompiles("""ConsumerProcessor[Id, String, WithConflictedMethods]""".stripMargin),
                        "Use unique argument name lists for overloaded methods."
                    )
                }
                "for trait with abstract type" in {
                    assertCompilationErrorMessage(
                        assertCompiles("""ConsumerProcessor[Id, String, WithAbstractType]"""),
                        "Abstract types are not supported: " +
                            "poppet.core.ProcessorSpec.WithAbstractType.A"
                    )
                }
                "for valid trait with ambiguous simple codec" in {
                    implicit val c0: Codec[String, Int] = a => Right(a.toInt)
                    implicit val c1: Codec[String, Int] = c0
                    assertCompilationErrorMessage(
                        assertCompiles("""ConsumerProcessor[Id, String, Simple]"""),
                        """ambiguous implicit values:
                          | both value c1 of type poppet.consumer.Codec[String,Int]
                          | and value c0 of type poppet.consumer.Codec[String,Int]
                          | match expected type poppet.Codec[String,Int]""".stripMargin,
                        "both value c1 and value c0 match type poppet.core.Codec[String, Int]"
                    )
                }
                "for valid trait without simple codec" in {
                    assertCompilationErrorMessagePattern(
                        assertCompiles("""ConsumerProcessor[Id, String, Simple]"""),
                        ("Unable to convert " +
                            "((cats.)?Id\\[(scala.Predef.|java.lang.)?String]|(scala.Predef.|java.lang.)?String) to " +
                            "(scala.)?Int. Try to provide poppet.Codec\\[(scala.Predef.|java.lang.)?String,(scala.)?Int].").r
                    )
                }
                "for valid trait without codec for type with argument" in {
                    implicit val c0: Codec[String, Int] = a => Right(a.toInt)
                    assertCompilationErrorMessagePattern(
                        assertCompiles("""ConsumerProcessor[Id, String, Simple]"""),
                        ("Unable to convert " +
                            "((cats.)?Id\\[(scala.Predef.|java.lang.)?String]|(scala.Predef.|java.lang.)?String) to " +
                            "(scala.collection.immutable.)?List\\[(scala.)?Int]. " +
                            "Try to provide " +
                            "poppet.Codec\\[(scala.Predef.|java.lang.)?String,(scala.collection.immutable.)?List\\[(scala.)?Int]] " +
                            "or " +
                            "poppet.CodecK\\[(\\[A\\])?(cats.)?Id(\\[A\\])?,(scala.collection.immutable.)?List].").r
                    )
                }
                "for valid trait without codec for simple type with explicit Id kind" in {
                    implicit val c0: Codec[String, Int] = a => Right(a.toInt)
                    implicit val c1: Codec[String, List[Int]] = a => Right(List(a.toInt))
                    implicit val c2: Codec[String, SimpleDto] = a => Right(SimpleDto(a.toInt))
                    assertCompilationErrorMessagePattern(
                        assertCompiles("""ConsumerProcessor[Id, String, Simple]"""),
                        ("Unable to convert " +
                            "((cats.)?Id\\[(scala.Predef.|java.lang.)?String]|(scala.Predef.|java.lang.)?String) to " +
                            "(cats.)?Id\\[(scala.collection.immutable.)?List\\[(scala.Predef.|java.lang.)?String]]. " +
                            "Try to provide " +
                            "poppet.Codec\\[(scala.Predef.|java.lang.)?String,(scala.collection.immutable.)?List\\[(scala.Predef.|java.lang.)?String]].").r
                    )
                }
                "for valid trait without codec for simple type with Future kind" in {
                    assertCompilationErrorMessagePattern(
                        assertCompiles("""ConsumerProcessor[Id, String, WithFutureKind]"""),
                        ("Unable to convert " +
                            "((cats.)?Id\\[(scala.Predef.|java.lang.)?String]|(scala.Predef.|java.lang.)?String) to " +
                            "scala.concurrent.Future\\[(scala.)?Int]. " +
                            "Try to provide " +
                            "poppet.Codec\\[(scala.Predef.|java.lang.)?String,scala.concurrent.Future\\[(scala.)?Int]] " +
                            "or " +
                            "poppet.CodecK\\[(\\[A\\])?(cats.)?Id(\\[A\\])?,(\\[\\+T\\])?scala.concurrent.Future(\\[T\\])?] " +
                            "with poppet.Codec\\[(scala.Predef.|java.lang.)?String,(scala.)?Int].").r
                    )
                }
                "for valid trait without codec for simple type with Future kind, but with codecK" in {
                    implicit val ck = new CodecK[Id, Future] {
                        override def apply[A](a: Id[A]): Future[A] = Future.successful(a)
                    }
                    assertCompilationErrorMessagePattern(
                        assertCompiles("""ConsumerProcessor[Id, String, WithFutureKind]"""),
                        ("Unable to convert " +
                            "((cats.)?Id\\[(scala.Predef.|java.lang.)?String]|(scala.Predef.|java.lang.)?String) to " +
                            "scala.concurrent.Future\\[(scala.)?Int]. " +
                            "Try to provide poppet.Codec\\[(scala.Predef.|java.lang.)?String,(scala.)?Int].").r
                    )
                }
                "for valid trait without codecK for simple type with Future kind, but with codec" in {
                    implicit val c0: Codec[String, Int] = a => Right(a.toInt)
                    assertCompilationErrorMessagePattern(
                        assertCompiles("""ConsumerProcessor[Id, String, WithFutureKind]"""),
                        ("Unable to convert " +
                            "((cats.)?Id\\[(scala.Predef.|java.lang.)?String]|(scala.Predef.|java.lang.)?String) to " +
                            "scala.concurrent.Future\\[(scala.)?Int]. " +
                            "Try to " +
                            "provide poppet.Codec\\[(scala.Predef.|java.lang.)?String,scala.concurrent.Future\\[(scala.)?Int]] " +
                            "or " +
                            "poppet.CodecK\\[(\\[A\\])?(cats.)?Id(\\[A\\])?,(\\[\\+T\\])?scala.concurrent.Future(\\[T\\])?].").r
                    )
                }
            }
            "when has Future data kind" - {
                "for valid trait without simple codec" in {
                    assertCompilationErrorMessagePattern(
                        assertCompiles("""ConsumerProcessor[Future, String, Simple]"""),
                        ("Unable to convert scala.concurrent.Future\\[(scala.Predef.|java.lang.)?String] to (scala.)?Int. " +
                            "Try to provide " +
                            "poppet.CodecK\\[(\\[\\+T\\])?scala.concurrent.Future(\\[T\\])?,(cats.)?Id] with " +
                            "poppet.Codec\\[(scala.Predef.|java.lang.)?String,(scala.)?Int].").r
                    )
                }
                "for valid trait without simple codec, but with codecK" in {
                    implicit val ck = new CodecK[Future, Id] {
                        override def apply[A](a: Future[A]): Id[A] = a.value.get.get
                    }
                    assertCompilationErrorMessagePattern(
                        assertCompiles("""ConsumerProcessor[Future, String, Simple]"""),
                        ("Unable to convert scala.concurrent.Future\\[(scala.Predef.|java.lang.)?String] to (scala.)?Int. " +
                            "Try to provide poppet.Codec\\[(scala.Predef.|java.lang.)?String,(scala.)?Int].").r
                    )
                }
                "for valid trait without simple codecK, but with codec" in {
                    implicit val c0: Codec[String, Int] = a => Right(a.toInt)
                    assertCompilationErrorMessagePattern(
                        assertCompiles("""ConsumerProcessor[Future, String, Simple]"""),
                        ("Unable to convert scala.concurrent.Future\\[(scala.Predef.|java.lang.)?String] to (scala.)?Int. " +
                            "Try to provide poppet.CodecK\\[(\\[\\+T\\])?scala.concurrent.Future(\\[T\\])?,(cats.)?Id].").r
                    )
                }
                "for valid trait without codec for type with argument" in {
                    implicit val ck = new CodecK[Future, Id] {
                        override def apply[A](a: Future[A]): Id[A] = a.value.get.get
                    }
                    implicit val c0: Codec[String, Int] = a => Right(a.toInt)
                    assertCompilationErrorMessagePattern(
                        assertCompiles("""ConsumerProcessor[Future, String, Simple]"""),
                        ("Unable to convert " +
                            "scala.concurrent.Future\\[(scala.Predef.|java.lang.)?String] to " +
                            "(scala.collection.immutable.)?List\\[(scala.)?Int]. " +
                            "Try to provide " +
                            "poppet.Codec\\[(scala.Predef.|java.lang.)?String,(scala.collection.immutable.)?List\\[(scala.)?Int]] " +
                            "or " +
                            "poppet.CodecK\\[(\\[\\+T\\])?scala.concurrent.Future(\\[T\\])?,(scala.collection.immutable.)?List].").r
                    )
                }
                "for valid trait without codec for simple type with explicit Id kind" in {
                    implicit val ck = new CodecK[Future, Id] {
                        override def apply[A](a: Future[A]): Id[A] = a.value.get.get
                    }
                    implicit val c0: Codec[String, Int] = a => Right(a.toInt)
                    implicit val c1: Codec[String, List[Int]] = a => Right(List(a.toInt))
                    implicit val c2: Codec[String, SimpleDto] = a => Right(SimpleDto(a.toInt))
                    assertCompilationErrorMessagePattern(
                        assertCompiles("""ConsumerProcessor[Future, String, Simple]"""),
                        ("Unable to convert " +
                            "scala.concurrent.Future\\[(scala.Predef.|java.lang.)?String] to " +
                            "(cats.)?Id\\[(scala.collection.immutable.)?List\\[(scala.Predef.|java.lang.)?String]]. " +
                            "Try to provide " +
                            "poppet.Codec\\[(scala.Predef.|java.lang.)?String,(scala.collection.immutable.)?List\\[(scala.Predef.|java.lang.)?String]].").r
                    )
                }
            }
        }
    }
}
