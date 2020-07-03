package poppet.consumer.spring

import cats.Id
import org.springframework.web.client.RestTemplate
import poppet.coder.ExchangeCoder
import poppet.consumer.Client
import poppet.dto.Request
import poppet.dto.Response

case class SpringClient(
    url: String)(restTemplate: RestTemplate
) extends Client[Array[Byte], Id] {
    override def materialize[I](
        coder: ExchangeCoder[Array[Byte], I, Id]
    ): Request[I] => Id[Response[I]] = request => {
        val erequest = coder.erequest(request)
        val response = restTemplate.postForEntity(url, erequest, classOf[Array[Byte]])
        coder.dresponse(response.getBody)
    }
}
