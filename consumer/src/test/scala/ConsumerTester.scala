import cats.implicits._
import poppet.coder.Coder
import poppet.consumer.ConsumerProcessor
import poppet.dto.Request
import poppet.dto.Response
import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits._
import scala.concurrent.Future
import scala.concurrent.duration.Duration

object ConsumerTester {
    val a = 31
    trait A {
        def a0: Int
        def a1(): Int
        def a2(b: Long): Int
        def a3(b1: Long, b2: Long): Int
        def a4(b1: Long, b2: Long)(b3: Long): Int
    }
    def main(args: Array[String]): Unit = {
        implicit val c1 = new Coder[Future[String], Int] {
            override def apply(a: Future[String]): Int = ???
        }
        implicit val c2 = new Coder[Long, Future[String]] {
            override def apply(a: Long): Future[String] = ???
        }

        println(ConsumerProcessor.apply[A].generate[String, Future]())
    }
}