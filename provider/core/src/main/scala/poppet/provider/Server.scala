package poppet.provider

trait Server[A, F[_], RQ, RS, M] {
    def buildRequest(request: RQ): F[A]
    def buildResponse(response: A): F[RS]
    def materialize(f: RQ => F[RS]): M
}
