package poppet.provider.spring

import cats.Id
import org.springframework.http.HttpStatus
import org.springframework.http.RequestEntity
import org.springframework.http.ResponseEntity
import poppet.coder.ExchangeCoder
import poppet.dto
import poppet.provider.Server

case class SpringServer()
    extends Server[Array[Byte], Id, RequestEntity[Array[Byte]] => ResponseEntity[Array[Byte]]] {
    override def materialize[I](
        coder: ExchangeCoder[Array[Byte], I, Id])(f: dto.Request[I] => Id[dto.Response[I]]
    ): RequestEntity[Array[Byte]] => ResponseEntity[Array[Byte]] = request => {
        val erequest = coder.drequest(request.getBody)
        val result = f(erequest)
        new ResponseEntity(coder.eresponse(result), HttpStatus.OK)
    }
}
