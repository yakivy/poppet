package poppet.provider

import poppet.CoreDsl

trait ProviderDsl extends CoreDsl {
    type Provider[I, F[_]] = core.Provider[I, F]
    type Server[I, F[_]] = I => F[I]

    val Provider = core.Provider
}
