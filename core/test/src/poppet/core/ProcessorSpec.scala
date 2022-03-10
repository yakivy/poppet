package poppet.core

import cats.Id
import org.scalatest.Assertion
import org.scalatest.exceptions.TestFailedException
import poppet.PoppetSpec
import scala.concurrent.Future

trait ProcessorSpec extends PoppetSpec {
    case class SimpleDto(value: Int)
    trait Simple {
        def a0: Int
        def a00(): List[Int]
        def a1(b: Boolean): SimpleDto
        def a2(b0: Boolean, b1: Boolean): Id[List[String]]
    }
    trait WithFutureKind {
        def a0: Future[Int]
        def a1: Future[List[Int]]
    }
    trait WithMultipleArgumentLists {
        def a0(b0: Boolean)(b1: Boolean): Int
        def a1(b0: Boolean)()(b1: Boolean): List[Int]
        def a2(b0: Boolean)(b10: Boolean, b11: Boolean): SimpleDto
    }
    trait WithDefaultArguments {
        def a0(b: Boolean = true): Int
        def a1(b0: Boolean, b1: Boolean = true): List[Int]
        def a2(b0: Boolean, b1: Boolean, b2: Boolean = true, b3: Boolean = true): SimpleDto
    }
    trait WithParameters[A, B] {
        def a0(b0: A): Int
        def a1: B
    }
    trait WithParentWithParameters extends WithParameters[Boolean, Int]
    trait WithComplexReturnTypes {
        def a(b: Boolean): WithComplexReturnTypes.ReturnType[Int]
        //def b: Either[String, Int]
    }
    object WithComplexReturnTypes {
        type ReturnType[A] = Either[String, A]
    }

    trait WithMethodWithParameters {
        def a[A](a: A): A
    }
    trait WithNoAbstractMethods {
        def a: Int = 1
    }
    trait WithConflictedMethods {
        def a(b: Int): Int
        def a(b: String): String
    }

    def assertCompilationErrorMessage(compilesAssert: => Assertion, message: String): Assertion = {
        try {
            compilesAssert
            fail("Compilation was successful")
        } catch { case e: TestFailedException =>
            assert(e.getMessage().contains(s""""$message""""))
        }
    }
}
