package poppet.provider.play

import akka.util.ByteString
import play.api.mvc.Action
import play.api.mvc.ActionBuilderImpl
import play.api.mvc.ControllerComponents
import play.api.mvc.Request
import play.api.mvc.Result
import play.api.mvc.Results
import poppet.provider.Server
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

case class PlayServer(
    cc: ControllerComponents)(implicit ec: ExecutionContext
) extends Server[Array[Byte], Future, Request[ByteString], Result, Action[ByteString]] {
    private val actionBuilder = new ActionBuilderImpl(cc.parsers.byteString)

    override def buildRequest(request: Request[ByteString]): Future[Array[Byte]] =
        Future.successful(request.body.toByteBuffer.array())

    override def buildResponse(response: Array[Byte]): Future[Result] =
        Future.successful(Results.Ok(response))

    override def materialize(f: Request[ByteString] => Future[Result]): Action[ByteString] =
        actionBuilder.async(f)
}
