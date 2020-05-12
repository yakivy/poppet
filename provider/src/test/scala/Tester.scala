import cats.Id
import poppet.coder.Coder
import poppet.provider.ProviderProcessor

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
            override def a0: Int = 31
            override def a1(): Int = 0
            override def a2(b: String): Int = 0
            override def a3(b1: String, b2: String): Int = 0
            override def a4(b1: String, b2: String)(b3: String): Int = 0
        }
        implicit val c1 = new Coder[Int, cats.Id[String]] {
            override def apply(a: Int): cats.Id[String] = ???
        }
        implicit val c2 = new Coder[String, cats.Id[String]] {
            override def apply(a: String): cats.Id[String] = ???
        }
        //println(ProviderProcessor.generate[A][java.lang.String, cats.Id](a))
        List(new _root_.poppet.provider.MethodProcessor[String, cats.Id]("a0", _root_.scala.Predef.Set(),
            ((as) => implicitly[_root_.poppet.coder.Coder[Int, cats.Id[String]]].apply(a.a0))),
            new _root_.poppet.provider.MethodProcessor[String, cats.Id]("a1", _root_.scala.Predef.Set(),
                ((as) => implicitly[_root_.poppet.coder.Coder[Int, cats.Id[String]]].apply(a.a1()))),
            new _root_.poppet.provider.MethodProcessor[String, cats.Id]("a2", _root_.scala.Predef.Set("b"),
                ((as) => cats.catsInstancesForId
                    .flatMap(implicitly[_root_.poppet.coder.Coder[String, cats.Id[String]]].apply(as("b")))(
                        ((b) => implicitly[_root_.poppet.coder.Coder[Int, cats.Id[String]]].apply(a.a2(b)))))),
            new _root_.poppet.provider.MethodProcessor[String, cats.Id]("a3", _root_.scala.Predef.Set("b1", "b2"),
                ((as) => cats.catsInstancesForId.flatMap(_root_.cats.Semigroupal
                    .tuple2(implicitly[_root_.poppet.coder.Coder[String, cats.Id[String]]].apply(as("b1")),
                        implicitly[_root_.poppet.coder.Coder[String, cats.Id[String]]].apply(as("b2")))) {
                    case scala.Tuple2(b1, b2)
                    => implicitly[_root_.poppet.coder.Coder[Int, cats.Id[String]]].apply(a.a3(b1, b2))
                }
                    )), new _root_.poppet.provider.MethodProcessor[String, cats.Id]("a4",
                _root_.scala.Predef.Set("b1", "b2", "b3"), ((as) => cats.catsInstancesForId.flatMap(
                    _root_.cats.Semigroupal
                        .tuple3(implicitly[_root_.poppet.coder.Coder[String, cats.Id[String]]].apply(as("b1")),
                            implicitly[_root_.poppet.coder.Coder[String, cats.Id[String]]].apply(as("b2")),
                            implicitly[_root_.poppet.coder.Coder[String, cats.Id[String]]].apply(as("b3")))) {
                    case scala.Tuple3(b1, b2, b3)
                    => implicitly[_root_.poppet.coder.Coder[Int, cats.Id[String]]].apply(a.a4(b1, b2)(b3))
                }
                    )))
    }
}