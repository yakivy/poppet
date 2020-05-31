import cats.implicits._
import poppet.coder.Coder
import poppet.provider.ProviderProcessor
import scala.concurrent.ExecutionContext.Implicits._
import scala.concurrent.Future

object Tester {
    trait A {
        def a0: Int
        def a1(): Int
        def a2(b: String): Int
        def a3(b1: String, b2: String): Int
        def a4(b1: String, b2: String)(b3: String): Int
    }
    def main(args: Array[String]): Unit = {
        val a = new A {
            override def a0: Int = 202
            override def a1(): Int = 0
            override def a2(b: String): Int = 0
            override def a3(b1: String, b2: String): Int = 0
            override def a4(b1: String, b2: String)(b3: String): Int = 0
        }
        implicit val c1 = new Coder[Int, Future[String]] {
            override def apply(a: Int): Future[String] = ???
        }
        implicit val c2 = new Coder[String, Future[String]] {
            override def apply(a: String): Future[String] = ???
        }
        println(ProviderProcessor.apply[A](a).generate[String, Future]())
    }
}