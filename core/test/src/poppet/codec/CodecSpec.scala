package poppet.codec

import poppet.core._
import poppet.codec.CodecSpec.A

trait CodecSpec {
    val stringExample = "a"
    val intExample = 1
    val caseClassExample = A(stringExample, intExample)

    def assertExchangeCodec[I](
        implicit sc: Codec[String, I],
        bqcodec: Codec[Request[I], I],
        iscodec: Codec[I, Response[I]],
        iqcodec: Codec[I, Request[I]],
        bscodec: Codec[Response[I], I],
        idcodec: Codec[I, I],
    ): Unit = {
        val request = Request[I]("A", "a", Map("b" -> sc("c").right.get))
        val response = Response[I](sc("c").right.get)
        val irequest = bqcodec(request).right.get
        assert(iqcodec(irequest).right.get == request)
        assert(iscodec(bscodec(response).right.get).right.get == response)
        assert(idcodec(irequest).right.get == irequest)
    }

    def assertCustomCodec[I, A](value: A)(
        implicit fc: Codec[A, I],
        bc: Codec[I, A],
    ): Unit = {
        val actual = bc(fc(value).right.get).right.get
        assert(actual == value, s"$actual != $value")
    }
}

object CodecSpec {
    case class A(a: String, b: Int)
}
