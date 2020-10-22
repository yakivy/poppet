package poppet.provider

trait ProviderDsl {
    type Provider[I, F[_]] = core.Provider[I, F]
    type ProviderProcessor[I, F[_]] = core.ProviderProcessor[I, F]
    type Server[F[_]] = Array[Byte] => F[Array[Byte]]

    val Provider = core.Provider
    val ProviderProcessor = core.ProviderProcessor
}
