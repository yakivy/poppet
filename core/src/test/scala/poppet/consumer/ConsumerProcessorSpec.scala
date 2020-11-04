package poppet.consumer

import cats.Id
import org.scalatest.FreeSpec
import poppet.consumer.core.ConsumerProcessor
import poppet.core.Request
import poppet.core.Response

class ConsumerProcessorSpec extends FreeSpec {
    "Consumer processor" - {
        trait A {
            def a0: Int
            def a00(): Int
            def a1(b: Boolean): Int
            def a2(b0: Boolean, b1: Boolean): Int
        }

        var request: Request[String] = null

        "when has id data kind, should generate instance" - {
            implicit val c0: Coder[String, Int] = a => a.toInt
            implicit val c1: Coder[Boolean, String] = a => a.toString

            val client: Request[String] => Response[String] = r => {
                request = r
                Response("0")
            }
            "for methods with different arguments number" in {
                val a = ConsumerProcessor[String, Id, A](client)

                assert(a.a0 == 0 && request == Request(
                    "poppet.consumer.ConsumerProcessorSpec.A", "a0", Map.empty
                ))
                assert(a.a00 == 0 && request == Request(
                    "poppet.consumer.ConsumerProcessorSpec.A", "a00", Map.empty
                ))
                assert(a.a1(true) == 0 && request == Request(
                    "poppet.consumer.ConsumerProcessorSpec.A", "a1", Map("b" -> "true")
                ))
                assert(a.a2(true, false) == 0 && request == Request(
                    "poppet.consumer.ConsumerProcessorSpec.A", "a2", Map("b0" -> "true", "b1" -> "false")
                ))
            }
            "for methods with multiple argument lists" in {
                trait B {
                    def a0(b0: Boolean)(b1: Boolean): Int
                    def a1(b0: Boolean)()(b1: Boolean): Int
                    def a2(b0: Boolean)(b10: Boolean, b11: Boolean): Int
                }
                val a = ConsumerProcessor[String, Id, B](client)

                assert(a.a0(true)(false) == 0 && request == Request(
                    "poppet.consumer.ConsumerProcessorSpec.B", "a0", Map("b0" -> "true", "b1" -> "false")
                ))
                assert(a.a1(true)()(false) == 0 && request == Request(
                    "poppet.consumer.ConsumerProcessorSpec.B", "a1", Map("b0" -> "true", "b1" -> "false")
                ))
                assert(a.a2(true)(false, true) == 0 && request == Request(
                    "poppet.consumer.ConsumerProcessorSpec.B", "a2", Map(
                        "b0" -> "true", "b10" -> "false", "b11" -> "true"
                    )
                ))
            }
            "for methods with default arguments" in {
                trait C {
                    def a0(b: Boolean = true): Int
                    def a1(b0: Boolean, b1: Boolean = true): Int
                    def a2(b0: Boolean, b1: Boolean, b2: Boolean = true, b3: Boolean = true): Int
                }

                val a: C = ConsumerProcessor[String, Id, C](client)

                assert(a.a0(false) == 0 && request == Request(
                    "poppet.consumer.ConsumerProcessorSpec.C", "a0", Map("b" -> "false")
                ))
                assert(a.a0() == 0 && request == Request(
                    "poppet.consumer.ConsumerProcessorSpec.C", "a0", Map("b" -> "true")
                ))
                assert(a.a1(false) == 0 && request == Request(
                    "poppet.consumer.ConsumerProcessorSpec.C", "a1", Map("b0" -> "false", "b1" -> "true")
                ))
                assert(a.a1(true, false) == 0 && request == Request(
                    "poppet.consumer.ConsumerProcessorSpec.C", "a1", Map("b0" -> "true", "b1" -> "false")
                ))
                assert(a.a2(false, false) == 0 && request == Request(
                    "poppet.consumer.ConsumerProcessorSpec.C", "a2", Map(
                        "b0" -> "false", "b1" -> "false", "b2" -> "true", "b3" -> "true"
                    )
                ))
                assert(a.a2(true, true, false) == 0 && request == Request(
                    "poppet.consumer.ConsumerProcessorSpec.C", "a2", Map(
                        "b0" -> "true", "b1" -> "true", "b2" -> "false", "b3" -> "true"
                    )
                ))
                assert(a.a2(true, true, false, false) == 0 && request == Request(
                    "poppet.consumer.ConsumerProcessorSpec.C", "a2", Map(
                        "b0" -> "true", "b1" -> "true", "b2" -> "false", "b3" -> "false"
                    )
                ))
            }
        }
        "when has future data kind, should generate instance" in {
            import cats.implicits._
            import scala.concurrent.ExecutionContext.Implicits.global
            import scala.concurrent.Await
            import scala.concurrent.Future
            import scala.concurrent.duration.Duration

            implicit val c0: Coder[Future[String], Int] = a => Await.result(a, Duration.Inf).toInt
            implicit val c1: Coder[Boolean, Future[String]] = a => Future.successful(a.toString)

            val a = ConsumerProcessor[String, Future, A](r => {
                request = r
                Future.successful(Response("0"))
            })

            assert(a.a0 == 0 && request == Request(
                "poppet.consumer.ConsumerProcessorSpec.A", "a0", Map.empty
            ))
            assert(a.a00 == 0 && request == Request(
                "poppet.consumer.ConsumerProcessorSpec.A", "a00", Map.empty
            ))
            assert(a.a1(true) == 0 && request == Request(
                "poppet.consumer.ConsumerProcessorSpec.A", "a1", Map("b" -> "true")
            ))
            assert(a.a2(true, false) == 0 && request == Request(
                "poppet.consumer.ConsumerProcessorSpec.A", "a2", Map("b0" -> "true", "b1" -> "false")
            ))
        }
        "when has A data kind and service has B data kind, should generate instance" in {
            import scala.util.Try
            import cats.implicits._

            type A[X] = Option[X]
            type B[Y] = Try[Y]

            implicit val c0: Coder[String, Int] = _.toInt
            implicit val c1: Coder[Boolean, String] = _.toString

            implicit def pureServerCoder[X, Y](implicit coder: Coder[X, Y]): Coder[X, A[Y]] =
                a => Option(coder(a))
            implicit def pureServiceCoder[X, Y](implicit coder: Coder[X, Y]): Coder[X, B[Y]] =
                a => Try(coder(a))
            implicit def pureServerLeftCoder[X, Y](implicit coder: Coder[X, B[Y]]): Coder[A[X], B[Y]] =
                a => a.map(coder.apply).get

            trait C {
                def a(b: Boolean): B[Int]
            }

            val p = ConsumerProcessor[String, A, C](r => {
                request = r
                Option(Response("0"))
            })

            assert(p.a(true) == Try(0) && request == Request(
                "poppet.consumer.ConsumerProcessorSpec.C", "a", Map("b" -> "true")
            ))
        }
    }
}
