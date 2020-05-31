package poppet

trait PlayProviderDsl extends ProviderDsl {
    type PlayServer = poppet.provider.play.PlayServer

    val PlayServer = poppet.provider.play.PlayServer
}
