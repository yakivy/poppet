package poppet.provider.play

import akka.util.ByteString
import play.api.mvc.Action
import play.api.mvc.ActionBuilderImpl
import play.api.mvc.ControllerComponents
import play.api.mvc.Results
import poppet.coder.ExchangeCoder
import poppet.dto
import poppet.provider.Server
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

case class PlayServer(
    cc: ControllerComponents)(implicit ec: ExecutionContext
) extends Server[Array[Byte], Future, Action[ByteString]] {
    private val actionBuilder = new ActionBuilderImpl(cc.parsers.byteString)

    override def materialize[I](
        coder: ExchangeCoder[Array[Byte], I, Future])(f: dto.Request[I] => Future[dto.Response[I]]
    ): Action[ByteString] = actionBuilder.async(request => for {
        request <- coder.drequest(
            request.body.toByteBuffer.array()
        )
        result <- f(request)
        response <- coder.eresponse(result)
    } yield Results.Ok(response))
}
