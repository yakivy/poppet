package poppet

trait Decorator[RQ, RS, F[_]] {
    def apply(chain: RQ => F[RS]): RQ => F[RS]
}
