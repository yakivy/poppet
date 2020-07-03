package poppet

trait SpringProviderDsl extends ProviderDsl {
    type SpringServer = poppet.provider.spring.SpringServer

    val SpringServer = poppet.provider.spring.SpringServer
}
