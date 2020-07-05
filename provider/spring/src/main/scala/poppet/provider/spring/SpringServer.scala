package poppet.provider.spring

import cats.Id
import org.springframework.http.HttpStatus
import org.springframework.http.RequestEntity
import org.springframework.http.ResponseEntity
import poppet.provider.Server

case class SpringServer() extends Server[
    Array[Byte], Id, RequestEntity[Array[Byte]], ResponseEntity[Array[Byte]],
    RequestEntity[Array[Byte]] => ResponseEntity[Array[Byte]]
] {
    override def buildRequest(request: RequestEntity[Array[Byte]]): Id[Array[Byte]] = request.getBody

    override def buildResponse(response: Array[Byte]): Id[ResponseEntity[Array[Byte]]] =
        new ResponseEntity(response, HttpStatus.OK)

    override def materialize(
        f: RequestEntity[Array[Byte]] => Id[ResponseEntity[Array[Byte]]]
    ): RequestEntity[Array[Byte]] => ResponseEntity[Array[Byte]] = f
}
