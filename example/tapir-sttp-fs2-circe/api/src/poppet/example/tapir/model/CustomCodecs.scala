package poppet.example.tapir.model

import cats.effect.IO
import cats.implicits._
import fs2.Stream
import io.circe.syntax._
import io.circe.Encoder
import io.circe.Json
import poppet.codec.circe.all._
import poppet.core.CodecFailure
import poppet.Codec

object CustomCodecs {
    implicit def fromSingleCodec[A](implicit subc: Codec[A, Json]): Codec[A, Either[Json, Stream[IO, Json]]] = a =>
        subc(a).map(_.asLeft[Stream[IO, Json]])

    implicit def fromStreamCodec[A: Encoder](implicit
        subc: Codec[A, Json]
    ): Codec[Stream[IO, A], Either[Json, Stream[IO, Json]]] = s =>
        s.evalMap(subc(_).fold[IO[Json]](IO.raiseError, IO.pure)).asRight[Json].asRight[CodecFailure[Stream[IO, A]]]

    implicit def toSingleCodec[A](implicit subc: Codec[Json, A]): Codec[Either[Json, Stream[IO, Json]], A] = {
        case Left(json) => subc(json).left.map(f => f.withData(f.data.asLeft[Stream[IO, Json]]))
        case Right(value) => new CodecFailure("Cannot convert stream to single", value.asRight[Json]).asLeft[A]
    }

    implicit def toStreamCodec[A](implicit
        subc: Codec[Json, A]
    ): Codec[Either[Json, Stream[IO, Json]], Stream[IO, A]] = {
        case Left(json) =>
            subc(json).bimap(f => f.withData(json.asLeft[Stream[IO, Json]]), Stream.emit(_).covary[IO])
        case Right(stream) =>
            stream
                .evalMap(a => subc(a).fold[IO[A]](IO.raiseError, IO.pure))
                .asRight[CodecFailure[Either[Json, Stream[IO, Json]]]]
    }

}
