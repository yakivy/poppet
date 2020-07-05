package poppet.consumer.spring

import cats.Id
import java.net.URI
import org.springframework.http.HttpMethod
import org.springframework.http.RequestEntity
import org.springframework.http.ResponseEntity
import org.springframework.web.client.RestTemplate
import poppet.consumer.Client

case class SpringClient(
    url: String)(restTemplate: RestTemplate
) extends Client[Array[Byte], Id, RequestEntity[Array[Byte]], ResponseEntity[Array[Byte]]] {
    override def buildRequest(request: Array[Byte]): Id[RequestEntity[Array[Byte]]] =
        new RequestEntity(request, HttpMethod.POST, URI.create(url))

    override def buildResponse(response: ResponseEntity[Array[Byte]]): Id[Array[Byte]] = response.getBody

    override def execute(request: RequestEntity[Array[Byte]]): Id[ResponseEntity[Array[Byte]]] =
        restTemplate.exchange(request, classOf[Array[Byte]])
}
