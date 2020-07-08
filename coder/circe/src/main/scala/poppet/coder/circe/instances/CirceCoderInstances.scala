package poppet.coder.circe.instances

import io.circe.Decoder
import io.circe.Encoder
import io.circe.Json
import poppet.coder.Coder
import poppet.coder.instances.CoderInstances

trait CirceCoderInstances extends CoderInstances {
    implicit def encoderToCoder[A](implicit encoder: Encoder[A]): Coder[A, Json] = encoder(_)
    implicit def decoderToCoder[A](implicit decoder: Decoder[A]): Coder[Json, A] =
        a => decoder(a.hcursor).fold(throw _, identity)
}
