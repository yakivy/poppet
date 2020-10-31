package poppet.consumer

import cats.Id
import org.scalatest.FreeSpec
import poppet.all._

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
            implicit val c0: ModelCoder[String, Int] = a => a.toInt
            implicit val c1: ModelCoder[Boolean, String] = a => a.toString

            val client: Request[String] => Response[String] = r => {
                request = r
                Response("0")
            }
            "for methods with different arguments number" in {
                val a = ConsumerProcessor[A].generate[String, Id]().process(client)

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
                val a = ConsumerProcessor[B].generate[String, Id]().process(client)

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

                val a: C = ConsumerProcessor[C].generate[String, Id]().process(client)

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

            implicit val c0: ModelCoder[Future[String], Int] = a => Await.result(a, Duration.Inf).toInt
            implicit val c1: ModelCoder[Boolean, Future[String]] = a => Future.successful(a.toString)

            val a = ConsumerProcessor[A].generate[String, Future]().process(r => {
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

            implicit val c0: ModelCoder[String, Int] = _.toInt
            implicit val c1: ModelCoder[Boolean, String] = _.toString

            implicit def pureServerModelCoder[X, Y](implicit ModelCoder: ModelCoder[X, Y]): ModelCoder[X, A[Y]] =
                a => Option(ModelCoder(a))
            implicit def pureServiceModelCoder[X, Y](implicit ModelCoder: ModelCoder[X, Y]): ModelCoder[X, B[Y]] =
                a => Try(ModelCoder(a))
            implicit def pureServerLeftModelCoder[X, Y](implicit
                ModelCoder: ModelCoder[X, B[Y]]): ModelCoder[A[X], B[Y]] =
                a => a.map(ModelCoder.apply).get

            trait C {
                def a(b: Boolean): B[Int]
            }

            val p = ConsumerProcessor[C].generate[String, A]().process(r => {
                request = r
                Option(Response("0"))
            })

            assert(p.a(true) == Try(0) && request == Request(
                "poppet.consumer.ConsumerProcessorSpec.C", "a", Map("b" -> "true")
            ))
        }
    }
}
