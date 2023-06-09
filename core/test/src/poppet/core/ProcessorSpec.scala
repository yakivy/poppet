package poppet.core

import cats.Id
import cats.data.EitherT
import org.scalatest.Assertion
import org.scalatest.exceptions.TestFailedException
import org.scalactic.source.Position
import poppet.PoppetSpec
import scala.concurrent.Future

object ProcessorSpec {
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
        def a0(b: Boolean): WithComplexReturnTypes.ReturnType[Int]
        def a1(b0: Boolean, b1: Boolean): WithComplexReturnTypes.ReturnType[Int]
        //def b: Either[String, Int]
    }
    object WithComplexReturnTypes {
        type ReturnType[A] = EitherT[Future, String, A]
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
    trait WithAbstractType {
        type A
        def a: String
    }
}

trait ProcessorSpec extends PoppetSpec {
    def assertCompilationErrorMessage(
        compilesAssert: => Assertion,
        message: String,
        alternativeMessages: String*
    )(implicit
        pos: Position
    ): Assertion = {
        try {
            compilesAssert
            fail("Compilation was successful")
        } catch { case e: TestFailedException =>
            val messages = (message :: alternativeMessages.toList).map(m => s""""$m"""")
            val candidateMessage = messages.find(e.getMessage.contains(_)).getOrElse(messages.head)
            assert(e.getMessage().contains(candidateMessage))
        }
    }
}
