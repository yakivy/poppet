package poppet

trait ProviderDsl {
    type Provider[A, I, M] = poppet.provider.Provider[A, I, M]
    type ProviderServiceProcessor[I] = poppet.provider.ProviderProcessor[I]

    val Provider = poppet.provider.Provider
    val ProviderProcessor = poppet.provider.ProviderProcessor
}
