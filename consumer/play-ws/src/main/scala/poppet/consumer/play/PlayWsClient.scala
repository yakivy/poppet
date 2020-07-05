package poppet.consumer.play

import play.api.libs.ws.WSClient
import play.api.libs.ws.WSRequest
import play.api.libs.ws.WSResponse
import poppet.consumer.Client
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

case class PlayWsClient(
    url: String)(wsClient: WSClient)(
    implicit ec: ExecutionContext
) extends Client[Array[Byte], Future, WSRequest, WSResponse] {
    override def buildRequest(request: Array[Byte]): Future[WSRequest] =
        Future.successful(wsClient.url(url).withBody(request).withMethod("POST"))

    override def buildResponse(response: WSResponse): Future[Array[Byte]] =
        Future.successful(response.bodyAsBytes.toByteBuffer.array())

    override def execute(request: WSRequest): Future[WSResponse] = request.execute()
}
