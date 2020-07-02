import cats.implicits._
import poppet.coder.Coder
import poppet.consumer.ConsumerProcessor
import poppet.dto.Response
import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits._
import scala.concurrent.Future
import scala.concurrent.duration.Duration

object ConsumerTester {
    val a = 29
    trait A {
        def a0: Int
        def a1(): Int
        def a2(b: String): Int
        def a3(b1: String, b2: String): Int
        def a4(b1: String, b2: String)(b3: String): Int
    }
    def main(args: Array[String]): Unit = {
        implicit val c1 = new Coder[Future[String], Int] {
            override def apply(a: Future[String]): Int = Await.result(a, Duration.Inf).toInt
        }
        println(ConsumerProcessor.apply[A].generate[String, Future]().f(
            r => {
                println(r)
                Future.successful(Response("1"))
            }
        ).a4("", "")(""))
    }
}