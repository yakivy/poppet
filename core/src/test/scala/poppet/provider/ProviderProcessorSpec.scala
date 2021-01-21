package poppet.provider

import cats.Id
import org.scalatest.FreeSpec
import poppet.provider.core.ProviderProcessor

class ProviderProcessorSpec extends FreeSpec {
    "Provider processor" - {
        trait A {
            def a0: Int
            def a00(): Int
            def a1(b: Boolean): Int
            def a2(b0: Boolean, b1: Boolean): Int
        }
        implicit class BooleanOps(value: Boolean) {
            def toInt = if (value) 1 else 0
        }
        val a: A = new A {
            override def a0: Int = 0
            override def a00(): Int = 0
            override def a1(b: Boolean): Int = b.toInt
            override def a2(b0: Boolean, b1: Boolean): Int = b0.toInt + b1.toInt
        }

        "when has id data kind" - {
            implicit val c0: Coder[Int, String] = _.toString
            implicit val c1: Coder[String, Boolean] = _.toBoolean

            "should generate instance" - {
                "for methods with different arguments number" in {
                    val p = ProviderProcessor[String, Id, A](a)

                    assert(p(0).service == "poppet.provider.ProviderProcessorSpec.A"
                        && p(0).name == "a2" && p(0).arguments == List("b0", "b1")
                        && p(0).f(Map("b0" -> "false", "b1" -> "true")) == "1")
                    assert(p(1).service == "poppet.provider.ProviderProcessorSpec.A"
                        && p(1).name == "a1" && p(1).arguments == List("b")
                        && p(1).f(Map("b" -> "true")) == "1")
                    assert(p(2).service == "poppet.provider.ProviderProcessorSpec.A"
                        && p(2).name == "a00" && p(2).arguments == List.empty
                        && p(2).f(Map.empty) == "0")
                    assert(p(3).service == "poppet.provider.ProviderProcessorSpec.A"
                        && p(3).name == "a0" && p(3).arguments == List.empty
                        && p(3).f(Map.empty) == "0")
                }
                "for methods with multiple argument lists" in {
                    trait B {
                        def a0(b0: Boolean)(b1: Boolean): Int
                        def a1(b0: Boolean)()(b1: Boolean): Int
                        def a2(b0: Boolean)(b10: Boolean, b11: Boolean): Int
                    }
                    val b: B = new B {
                        override def a0(b0: Boolean)(b1: Boolean): Int = b0.toInt + b1.toInt
                        override def a1(b0: Boolean)()(b1: Boolean): Int = b0.toInt + b1.toInt
                        override def a2(b0: Boolean)(b10: Boolean, b11: Boolean): Int =
                            b0.toInt + b10.toInt + b11.toInt
                    }

                    val p = ProviderProcessor[String, Id, B](b)

                    assert(p(0).service == "poppet.provider.ProviderProcessorSpec.B"
                        && p(0).name == "a2" && p(0).arguments == List("b0", "b10", "b11")
                        && p(0).f(Map("b0" -> "true", "b10" -> "true", "b11" -> "true")) == "3")
                    assert(p(1).service == "poppet.provider.ProviderProcessorSpec.B"
                        && p(1).name == "a1" && p(1).arguments == List("b0", "b1")
                        && p(1).f(Map("b0" -> "false", "b1" -> "false")) == "0")
                    assert(p(2).service == "poppet.provider.ProviderProcessorSpec.B"
                        && p(2).name == "a0" && p(2).arguments == List("b0", "b1")
                        && p(2).f(Map("b0" -> "true", "b1" -> "false")) == "1")
                }
                "for methods with default arguments" in {
                    trait C {
                        def a0(b: Boolean = true): Int
                        def a1(b0: Boolean, b1: Boolean = true): Int
                        def a2(b0: Boolean, b1: Boolean, b2: Boolean = true, b3: Boolean = true): Int
                    }

                    val c: C = new C {
                        override def a0(b: Boolean): Int = b.toInt
                        override def a1(b0: Boolean, b1: Boolean): Int = b0.toInt + b1.toInt
                        override def a2(b0: Boolean, b1: Boolean, b2: Boolean, b3: Boolean): Int =
                            b0.toInt + b1.toInt + b2.toInt + b3.toInt
                    }

                    val p = ProviderProcessor[String, Id, C](c)

                    assert(p(0).service == "poppet.provider.ProviderProcessorSpec.C"
                        && p(0).name == "a2" && p(0).arguments == List("b0", "b1", "b2", "b3")
                        && p(0).f(Map("b0" -> "true", "b1" -> "true", "b2" -> "true", "b3" -> "true")) == "4")
                    assert(p(1).service == "poppet.provider.ProviderProcessorSpec.C"
                        && p(1).name == "a1" && p(1).arguments == List("b0", "b1")
                        && p(1).f(Map("b0" -> "true", "b1" -> "false")) == "1")
                    assert(p(2).service == "poppet.provider.ProviderProcessorSpec.C"
                        && p(2).name == "a0" && p(2).arguments == List("b")
                        && p(2).f(Map("b" -> "false")) == "0")
                }
                "for traits with generic hierarchy" in {
                    trait C[X, Y] {
                        def a0(b0: X): Int
                        def a1: Y
                    }
                    trait D extends C[Boolean, Int]
                    val d: D = new D {
                        override def a0(b0: Boolean): Int = b0.toInt
                        override def a1: Int = 1
                    }
                    val p = ProviderProcessor[String, Id, D](d)
                    assert(p(0).service == "poppet.provider.ProviderProcessorSpec.D"
                        && p(0).name == "a1" && p(0).arguments == List.empty
                        && p(0).f(Map.empty) == "1")
                    assert(p(1).service == "poppet.provider.ProviderProcessorSpec.D"
                        && p(1).name == "a0" && p(1).arguments == List("b0")
                        && p(1).f(Map("b0" -> "true")) == "1")
                }
            }
        }
        "when has future data kind" - {
            import cats.implicits._
            import scala.concurrent.ExecutionContext.Implicits.global
            import scala.concurrent.Await
            import scala.concurrent.Future
            import scala.concurrent.duration.Duration

            implicit val c0: Coder[Int, Future[String]] = a => Future.successful(a.toString)
            implicit val c1: Coder[String, Future[Boolean]] = a => Future.successful(a.toBoolean)

            "should generate instance" in {
                val p = ProviderProcessor[String, Future, A](a)

                def result[A](value: Future[A]): A = Await.result[A](value, Duration.Inf)

                assert(p(0).service == "poppet.provider.ProviderProcessorSpec.A"
                    && p(0).name == "a2" && p(0).arguments == List("b0", "b1")
                    && result(p(0).f(Map("b0" -> "true", "b1" -> "true"))) == "2")
                assert(p(1).service == "poppet.provider.ProviderProcessorSpec.A"
                    && p(1).name == "a1" && p(1).arguments == List("b")
                    && result(p(1).f(Map("b" -> "true"))) == "1")
                assert(p(2).service == "poppet.provider.ProviderProcessorSpec.A"
                    && p(2).name == "a00" && p(2).arguments == List.empty
                    && result(p(2).f(Map.empty)) == "0")
                assert(p(3).service == "poppet.provider.ProviderProcessorSpec.A"
                    && p(3).name == "a0" && p(3).arguments == List.empty
                    && result(p(3).f(Map.empty)) == "0")
            }
        }
        "when has A data kind and service has B data kind should generate instance" in {
            import scala.util.Try
            import cats.implicits._

            type A[X] = Option[X]
            type B[Y] = Try[Y]

            implicit val c0: Coder[Int, String] = _.toString
            implicit val c1: Coder[String, Boolean] = _.toBoolean

            implicit def pureServerCoder[X, Y](implicit coder: Coder[X, Y]): Coder[X, A[Y]] =
                a => Option(coder(a))
            implicit def pureServiceLeftCoder[X, Y](implicit coder: Coder[X, A[Y]]): Coder[B[X], A[Y]] =
                a => a.map(coder.apply).get

            trait C {
                def a(b: Boolean): B[Int]
            }
            val c: C = new C {
                override def a(b: Boolean): B[Int] = Try(b.toInt)
            }

            val p = ProviderProcessor[String, A, C](c)

            def result[X](value: A[X]): X = value.get

            assert(p(0).service == "poppet.provider.ProviderProcessorSpec.C"
                && p(0).name == "a" && p(0).arguments == List("b")
                && result(p(0).f(Map("b" -> "true"))) == "1")
        }
    }
}