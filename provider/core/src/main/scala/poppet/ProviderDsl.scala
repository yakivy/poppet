package poppet

trait ProviderDsl {
    type Provider[A, I, F[_], RQ, RS, M] = poppet.provider.Provider[A, I, F, RQ, RS, M]
    type ProviderServiceProcessor[I, F[_]] = poppet.provider.ProviderProcessor[I, F]

    val Provider = poppet.provider.Provider
    val ProviderProcessor = poppet.provider.ProviderProcessor
}
