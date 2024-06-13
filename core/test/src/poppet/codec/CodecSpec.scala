package poppet.codec

import poppet.codec.CodecSpec.A
import poppet.core._

trait CodecSpec {
    val stringExample = "a"
    val intExample = 1
    val caseClassExample = A(stringExample, intExample)

    def assertCustomCodec[I, A](value: A)(implicit
        fc: Codec[A, I],
        bc: Codec[I, A],
    ): Unit = {
        val actual = fc(value).flatMap(bc(_))
        assert(actual == Right(value), s"$actual != ${Right(value)}")
    }

}

object CodecSpec {
    case class A(a: String, b: Int)
}
