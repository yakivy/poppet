package poppet.consumer.play

import play.api.libs.ws.WSClient
import poppet.coder.ExchangeCoder
import poppet.consumer.Client
import poppet.dto.Request
import poppet.dto.Response
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

case class PlayClient(
    url: String)(wsClient: WSClient)(
    implicit ec: ExecutionContext
) extends Client[Array[Byte], Future] {
    override def materialize[I](
        coder: ExchangeCoder[Array[Byte], I, Future]
    ): Request[I] => Future[Response[I]] = request => for {
        erequest <- coder.erequest(request)
        response <- wsClient.url(url).post(erequest)
        dresponse <- coder.dresponse(response.bodyAsBytes.toByteBuffer.array())
    } yield dresponse
}
