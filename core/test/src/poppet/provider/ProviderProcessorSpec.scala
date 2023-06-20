package poppet.provider

import cats._
import cats.data.EitherT
import org.scalatest.freespec.AsyncFreeSpec
import poppet.core.ProcessorSpec
import poppet.core.ProcessorSpec._
import poppet.provider.core.ProviderProcessor
import scala.concurrent.Future

class ProviderProcessorSpec extends AsyncFreeSpec with ProcessorSpec {
    "Provider processor" - {
        implicit class BooleanOps(value: Boolean) {
            def toInt = if (value) 1 else 0
        }
        val simpleImpl: Simple = new Simple {
            override def a0: Int = 0
            override def a00(): List[Int] = List(1)
            override def a1(b: Boolean): SimpleDto = SimpleDto(2)
            override def a2(b0: Boolean, b1: Option[Boolean]): Id[List[String]] =
                List((b0.toInt + b1.getOrElse(false).toInt).toString)
        }
        val withComplexReturnTypesImpl = new WithComplexReturnTypes {
            override def a0(b: Boolean): WithComplexReturnTypes.ReturnType[Int] =
                EitherT.fromEither(Right(b.toInt))
            override def a1(b0: Boolean, b1: Boolean): WithComplexReturnTypes.ReturnType[Int] =
                EitherT.fromEither(Right(b0.toInt + b1.toInt))
        }

        "should generate instance" - {
            implicit val c0: Codec[Int, String] = a => Right(a.toString)
            implicit val c1: Codec[List[Int], String] = a => Right(a.toString)
            implicit val c2: Codec[SimpleDto, String] = a => Right(a.toString)
            implicit val c3: Codec[List[String], String] = a => Right(a.toString)
            implicit val cp0: Codec[String, Boolean] = a => Right(a.toBoolean)
            implicit val cp1: Codec[String, Option[Boolean]] = a => Right(Option(a.toBoolean))

            "when has id data kind" - {
                "for methods with different arguments number" in {
                    val p = ProviderProcessor[Id, String, Simple].apply(simpleImpl, FailureHandler.throwing)

                    assert(p(0).service == "poppet.core.ProcessorSpec.Simple"
                        && p(0).name == "a0" && p(0).arguments == List.empty
                        && p(0).f(Map.empty) == "0")
                    assert(p(1).service == "poppet.core.ProcessorSpec.Simple"
                        && p(1).name == "a00" && p(1).arguments == List.empty
                        && p(1).f(Map.empty) == "List(1)")
                    assert(p(2).service == "poppet.core.ProcessorSpec.Simple"
                        && p(2).name == "a1" && p(2).arguments == List("b")
                        && p(2).f(Map("b" -> "true")) == "SimpleDto(2)")
                    assert(p(3).service == "poppet.core.ProcessorSpec.Simple"
                        && p(3).name == "a2" && p(3).arguments == List("b0", "b1")
                        && p(3).f(Map("b0" -> "false", "b1" -> "true")) == "List(1)")
                }
                "for methods with future kind" in {
                    implicit val ck: CodecK[Future, Id] = new CodecK[Future, Id] {
                        override def apply[A](a: Future[A]): Id[A] = a.value.get.get
                    }
                    val t = new WithFutureKind {
                        override def a0: Future[Int] = Future.successful(1)
                        override def a1: Future[List[Int]] = Future.successful(List(1))
                    }

                    val p = ProviderProcessor[Id, String, WithFutureKind].apply(t, FailureHandler.throwing)

                    assert(p(0).service == "poppet.core.ProcessorSpec.WithFutureKind"
                        && p(0).name == "a0" && p(0).arguments == List.empty
                        && p(0).f(Map.empty) == "1")
                    assert(p(1).service == "poppet.core.ProcessorSpec.WithFutureKind"
                        && p(1).name == "a1" && p(1).arguments == List.empty
                        && p(1).f(Map.empty) == "List(1)")
                }
                "for methods with multiple argument lists" in {
                    val t: WithMultipleArgumentLists = new WithMultipleArgumentLists {
                        override def a0(b0: Boolean)(b1: Boolean): Int = b0.toInt + b1.toInt
                        override def a1(b0: Boolean)()(b1: Boolean): List[Int] = List(b0.toInt, b1.toInt)
                        override def a2(b0: Boolean)(b10: Boolean, b11: Boolean): SimpleDto =
                            SimpleDto(b0.toInt + b10.toInt + b11.toInt)
                    }

                    val p = ProviderProcessor[Id, String, WithMultipleArgumentLists].apply(t, FailureHandler.throwing)

                    assert(p(0).service == "poppet.core.ProcessorSpec.WithMultipleArgumentLists"
                        && p(0).name == "a0" && p(0).arguments == List("b0", "b1")
                        && p(0).f(Map("b0" -> "true", "b1" -> "false")) == "1")
                    assert(p(1).service == "poppet.core.ProcessorSpec.WithMultipleArgumentLists"
                        && p(1).name == "a1" && p(1).arguments == List("b0", "b1")
                        && p(1).f(Map("b0" -> "false", "b1" -> "false")) == "List(0, 0)")
                    assert(p(2).service == "poppet.core.ProcessorSpec.WithMultipleArgumentLists"
                        && p(2).name == "a2" && p(2).arguments == List("b0", "b10", "b11")
                        && p(2).f(Map("b0" -> "true", "b10" -> "true", "b11" -> "true")) == "SimpleDto(3)")
                }
                "for methods with default arguments" in {
                    val t: WithDefaultArguments = new WithDefaultArguments {
                        override def a0(b: Boolean): Int = b.toInt
                        override def a1(b0: Boolean, b1: Boolean): List[Int] = List(b0.toInt, b1.toInt)
                        override def a2(b0: Boolean, b1: Boolean, b2: Boolean, b3: Boolean): SimpleDto =
                            SimpleDto(b0.toInt + b1.toInt + b2.toInt + b3.toInt)
                    }

                    val p = ProviderProcessor[Id, String, WithDefaultArguments].apply(t, FailureHandler.throwing)

                    assert(p(0).service == "poppet.core.ProcessorSpec.WithDefaultArguments"
                        && p(0).name == "a0" && p(0).arguments == List("b")
                        && p(0).f(Map("b" -> "false")) == "0")
                    assert(p(1).service == "poppet.core.ProcessorSpec.WithDefaultArguments"
                        && p(1).name == "a1" && p(1).arguments == List("b0", "b1")
                        && p(1).f(Map("b0" -> "true", "b1" -> "false")) == "List(1, 0)")
                    assert(p(2).service == "poppet.core.ProcessorSpec.WithDefaultArguments"
                        && p(2).name == "a2" && p(2).arguments == List("b0", "b1", "b2", "b3")
                        && p(2).f(Map("b0" -> "true", "b1" -> "true", "b2" -> "true", "b3" -> "true")) == "SimpleDto(4)")
                }
                "for traits with generic hierarchy" in {
                    val t: WithParentWithParameters = new WithParentWithParameters {
                        override def a0(b0: Boolean): Int = b0.toInt
                        override def a1: Int = 1
                    }
                    val p = ProviderProcessor[Id, String, WithParentWithParameters].apply(t, FailureHandler.throwing)
                    assert(p(0).service == "poppet.core.ProcessorSpec.WithParentWithParameters"
                        && p(0).name == "a0" && p(0).arguments == List("b0")
                        && p(0).f(Map("b0" -> "true")) == "1")
                    assert(p(1).service == "poppet.core.ProcessorSpec.WithParentWithParameters"
                        && p(1).name == "a1" && p(1).arguments == List.empty
                        && p(1).f(Map.empty) == "1")
                }
            }
            "when has future data kind" in {
                import cats.implicits._
                import scala.concurrent.Future

                implicit val ck0: CodecK[cats.Id, Future] = new CodecK[cats.Id, Future] {
                    override def apply[A](a: Id[A]): Future[A] = Future.successful(a)
                }

                val p = ProviderProcessor[Future, String, Simple].apply(simpleImpl, FailureHandler.throwing)

                p(0).f(Map.empty).map(result =>
                    assert(p(0).service == "poppet.core.ProcessorSpec.Simple"
                        && p(0).name == "a0" && p(0).arguments == List.empty
                        && result == "0"))
                p(1).f(Map.empty).map(result =>
                    assert(p(1).service == "poppet.core.ProcessorSpec.Simple"
                        && p(1).name == "a00" && p(1).arguments == List.empty
                        && result == "List(0)"))
                p(2).f(Map("b" -> "true")).map(result =>
                    assert(p(2).service == "poppet.core.ProcessorSpec.Simple"
                        && p(2).name == "a1" && p(2).arguments == List("b")
                        && result == "1"))
                p(3).f(Map("b0" -> "true", "b1" -> "true")).map(result =>
                    assert(p(3).service == "poppet.core.ProcessorSpec.Simple"
                        && p(3).name == "a2" && p(3).arguments == List("b0", "b1")
                        && result == "List(2)")
                )
            }
            "when has complex data kind" in {
                import cats.implicits._

                val p = ProviderProcessor[WithComplexReturnTypes.ReturnType, String, WithComplexReturnTypes]
                    .apply(withComplexReturnTypesImpl, FailureHandler.throwing)

                def result[A](value: WithComplexReturnTypes.ReturnType[A]): A =
                    value.value.value.get.get.toOption.get

                assert(p(0).service == "poppet.core.ProcessorSpec.WithComplexReturnTypes"
                    && p(0).name == "a0" && p(0).arguments == List("b")
                    && result(p(0).f(Map("b" -> "true"))) == "1")
                assert(p(1).service == "poppet.core.ProcessorSpec.WithComplexReturnTypes"
                    && p(1).name == "a1" && p(1).arguments == List("b0", "b1")
                    && result(p(1).f(Map("b0" -> "true", "b1" -> "true"))) == "2")
            }
            "when has A data kind and service has B data kind" in {
                import scala.util.Try
                import cats.implicits._

                type F[A] = Option[A]
                type G[A] = WithComplexReturnTypes.ReturnType[A]

                implicit val ck0: CodecK[F, G] = new CodecK[F, G] {
                    override def apply[A](a: F[A]): G[A] = EitherT.fromEither(a.toRight("not found"))
                }
                implicit val ck1: CodecK[G, F] = new CodecK[G, F] {
                    override def apply[A](a: G[A]): F[A] = a.value.value.get.get.toOption
                }

                val p = ProviderProcessor[F, String, WithComplexReturnTypes]
                    .apply(withComplexReturnTypesImpl, FailureHandler.throwing)

                def result[A](value: F[A]): A = value.get

                assert(p(0).service == "poppet.core.ProcessorSpec.WithComplexReturnTypes"
                    && p(0).name == "a0" && p(0).arguments == List("b")
                    && result(p(0).f(Map("b" -> "true"))) == "1")
                assert(p(1).service == "poppet.core.ProcessorSpec.WithComplexReturnTypes"
                    && p(1).name == "a1" && p(1).arguments == List("b0", "b1")
                    && result(p(1).f(Map("b0" -> "true", "b1" -> "true"))) == "2")
            }
        }
        "shouldn't generate instance" - {
            "when has id data kind" - {
                "for trait with generic methods" in {
                    assertCompilationErrorMessage(
                        assertCompiles("""ProviderProcessor[Id, String, WithMethodWithParameters]"""),
                        "Generic methods are not supported: " +
                            "poppet.core.ProcessorSpec.WithMethodWithParameters.a"
                    )
                }
                "for trait without abstract methods" in {
                    assertCompilationErrorMessage(
                        assertCompiles("""ProviderProcessor[Id, String, WithNoAbstractMethods]"""),
                        "poppet.core.ProcessorSpec.WithNoAbstractMethods has no abstract methods. " +
                            "Make sure that service method is parametrized with a trait."
                    )
                }
                "for trait with conflicting methods" in {
                    assertCompilationErrorMessage(
                        assertCompiles("""ProviderProcessor[Id, String, WithConflictedMethods]"""),
                        "Use unique argument name lists for overloaded methods.",
                    )
                }
                "for trait with abstract type" in {
                    assertCompilationErrorMessage(
                        assertCompiles("""ProviderProcessor[Id, String, WithAbstractType]"""),
                        "Abstract types are not supported: " +
                            "poppet.core.ProcessorSpec.WithAbstractType.A"
                    )
                }
                "for valid trait without simple codec" in {
                    assertCompilationErrorMessage(
                        assertCompiles("""ProviderProcessor[Id, String, Simple]"""),
                        "Unable to convert Int to cats.Id[String]. Try to provide poppet.Codec[Int,String].",
                        "Unable to convert scala.Int to cats.Id[scala.Predef.String]. " +
                            "Try to provide poppet.Codec[scala.Int,scala.Predef.String].",
                    )
                }
                "for valid trait without codec for type with argument" in {
                    implicit val c0: Codec[Int, String] = a => Right(a.toString)
                    assertCompilationErrorMessage(
                        assertCompiles("""ProviderProcessor[Id, String, Simple]"""),
                        "Unable to convert List[Int] to cats.Id[String]. " +
                            "Try to provide poppet.Codec[List[Int],String] or poppet.CodecK[List,cats.Id].",
                        "Unable to convert scala.collection.immutable.List[scala.Int] to cats.Id[scala.Predef.String]. " +
                            "Try to provide poppet.Codec[scala.collection.immutable.List[scala.Int],scala.Predef.String] or " +
                            "poppet.CodecK[scala.collection.immutable.List,cats.Id].",
                    )
                }
                "for valid trait without codec for simple type with explicit Id kind" in {
                    implicit val c0: Codec[Int, String] = a => Right(a.toString)
                    implicit val c1: Codec[List[Int], String] = a => Right(a.toString)
                    implicit val c2: Codec[SimpleDto, String] = a => Right(a.toString)
                    assertCompilationErrorMessage(
                        assertCompiles("""ProviderProcessor[Id, String, Simple]"""),
                        "Unable to convert cats.Id[List[String]] to cats.Id[String]. " +
                            "Try to provide poppet.Codec[List[String],String].",
                        "Unable to convert cats.Id[scala.collection.immutable.List[scala.Predef.String]] to cats.Id[scala.Predef.String]. " +
                            "Try to provide poppet.Codec[scala.collection.immutable.List[scala.Predef.String],scala.Predef.String].",
                    )
                }
                "for valid trait without codec for simple type with Future kind" in {
                    assertCompilationErrorMessage(
                        assertCompiles("""ProviderProcessor[Id, String, WithFutureKind]"""),
                        "Unable to convert scala.concurrent.Future[Int] to cats.Id[String]. " +
                            "Try to provide poppet.Codec[scala.concurrent.Future[Int],String] " +
                            "or poppet.CodecK[scala.concurrent.Future,cats.Id] with poppet.Codec[Int,String].",
                        "Unable to convert scala.concurrent.Future[scala.Int] to cats.Id[scala.Predef.String]. " +
                            "Try to provide poppet.Codec[scala.concurrent.Future[scala.Int],scala.Predef.String] " +
                            "or poppet.CodecK[scala.concurrent.Future,cats.Id] with poppet.Codec[scala.Int,scala.Predef.String].",
                    )
                }
                "for valid trait without codec for simple type with Future kind, but with codecK" in {
                    implicit val ck = new CodecK[Future, Id] {
                        override def apply[A](a: Future[A]): Id[A] = a.value.get.get
                    }
                    assertCompilationErrorMessage(
                        assertCompiles("""ProviderProcessor[Id, String, WithFutureKind]"""),
                        "Unable to convert scala.concurrent.Future[Int] to cats.Id[String]. " +
                            "Try to provide poppet.Codec[Int,String].",
                        "Unable to convert scala.concurrent.Future[scala.Int] to cats.Id[scala.Predef.String]. " +
                            "Try to provide poppet.Codec[scala.Int,scala.Predef.String].",
                    )
                }
                "for valid trait without codecK for simple type with Future kind, but with codec" in {
                    implicit val c0: Codec[Int, String] = a => Right(a.toString)
                    assertCompilationErrorMessage(
                        assertCompiles("""ProviderProcessor[Id, String, WithFutureKind]"""),
                        "Unable to convert scala.concurrent.Future[Int] to cats.Id[String]. " +
                            "Try to provide poppet.Codec[scala.concurrent.Future[Int],String] " +
                            "or poppet.CodecK[scala.concurrent.Future,cats.Id].",
                        "Unable to convert scala.concurrent.Future[scala.Int] to cats.Id[scala.Predef.String]. " +
                            "Try to provide poppet.Codec[scala.concurrent.Future[scala.Int],scala.Predef.String] " +
                            "or poppet.CodecK[scala.concurrent.Future,cats.Id].",
                    )
                }
            }
            "when has Future data kind" - {
                "for valid trait without simple codec" in {
                    assertCompilationErrorMessage(
                        assertCompiles("""ProviderProcessor[Future, String, Simple]"""),
                        "Unable to convert Int to scala.concurrent.Future[String]. " +
                            "Try to provide poppet.CodecK[cats.Id,scala.concurrent.Future] with poppet.Codec[Int,String].",
                        "Unable to convert scala.Int to scala.concurrent.Future[scala.Predef.String]. " +
                            "Try to provide poppet.CodecK[cats.Id,scala.concurrent.Future] with poppet.Codec[scala.Int,scala.Predef.String].",
                    )
                }
                "for valid trait without simple codec, but with codecK" in {
                    implicit val ck = new CodecK[Id, Future] {
                        override def apply[A](a: Id[A]): Future[A] = Future.successful(a)
                    }
                    assertCompilationErrorMessage(
                        assertCompiles("""ProviderProcessor[Future, String, Simple]"""),
                        "Unable to convert Int to scala.concurrent.Future[String]. " +
                            "Try to provide poppet.Codec[Int,String].",
                        "Unable to convert scala.Int to scala.concurrent.Future[scala.Predef.String]. " +
                            "Try to provide poppet.Codec[scala.Int,scala.Predef.String].",
                    )
                }
                "for valid trait without simple codecK, but with codec" in {
                    implicit val c0: Codec[Int, String] = a => Right(a.toString)
                    assertCompilationErrorMessage(
                        assertCompiles("""ProviderProcessor[Future, String, Simple]"""),
                        "Unable to convert Int to scala.concurrent.Future[String]. " +
                            "Try to provide poppet.CodecK[cats.Id,scala.concurrent.Future].",
                        "Unable to convert scala.Int to scala.concurrent.Future[scala.Predef.String]. " +
                            "Try to provide poppet.CodecK[cats.Id,scala.concurrent.Future].",
                    )
                }
                "for valid trait without codec for type with argument" in {
                    implicit val ck = new CodecK[Id, Future] {
                        override def apply[A](a: Id[A]): Future[A] = Future.successful(a)
                    }
                    implicit val c0: Codec[Int, String] = a => Right(a.toString)
                    assertCompilationErrorMessage(
                        assertCompiles("""ProviderProcessor[Future, String, Simple]"""),
                        "Unable to convert List[Int] to scala.concurrent.Future[String]. " +
                            "Try to provide poppet.Codec[List[Int],String] or poppet.CodecK[List,scala.concurrent.Future].",
                        "Unable to convert scala.collection.immutable.List[scala.Int] to scala.concurrent.Future[scala.Predef.String]. " +
                            "Try to provide poppet.Codec[scala.collection.immutable.List[scala.Int],scala.Predef.String] or " +
                            "poppet.CodecK[scala.collection.immutable.List,scala.concurrent.Future].",
                    )
                }
                "for valid trait without codec for simple type with explicit Id kind" in {
                    implicit val ck = new CodecK[Id, Future] {
                        override def apply[A](a: Id[A]): Future[A] = Future.successful(a)
                    }
                    implicit val c0: Codec[Int, String] = a => Right(a.toString)
                    implicit val c1: Codec[List[Int], String] = a => Right(a.toString)
                    implicit val c2: Codec[SimpleDto, String] = a => Right(a.toString)
                    assertCompilationErrorMessage(
                        assertCompiles("""ProviderProcessor[Future, String, Simple]"""),
                        "Unable to convert cats.Id[List[String]] to scala.concurrent.Future[String]. " +
                            "Try to provide poppet.Codec[List[String],String].",
                        "Unable to convert cats.Id[scala.collection.immutable.List[scala.Predef.String]] to scala.concurrent.Future[scala.Predef.String]. " +
                            "Try to provide poppet.Codec[scala.collection.immutable.List[scala.Predef.String],scala.Predef.String].",
                    )
                }
            }
        }
    }
}