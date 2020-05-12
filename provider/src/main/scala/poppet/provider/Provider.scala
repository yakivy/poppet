package poppet.provider

import poppet.coder.BiCoder

case class Provider[A, I, F[_], M](
    server: Server[A, F, M], coder: BiCoder[A, I, F])(
    processors: ProviderProcessor[I, F]*
) {
    def materialize(): M = server.materialize(coder) { r =>
        ???
    }
}
