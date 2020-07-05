package poppet.consumer

trait Client[A, F[_], RQ, RS] {
    def buildRequest(request: A): F[RQ]
    def buildResponse(response: RS): F[A]
    def execute(request: RQ): F[RS]
}
