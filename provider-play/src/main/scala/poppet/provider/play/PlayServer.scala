package poppet.provider.play

import play.api.mvc.Action
import play.api.mvc.AnyContent
import poppet.coder.BiCoder
import poppet.dto
import poppet.provider.Server
import scala.concurrent.Future

case class PlayServer() extends Server[Array[Byte], Future, Action[AnyContent]] {
    override def materialize[I](
        coder: BiCoder[Array[Byte], I, Future])(f: dto.Request[I] => dto.Response[I]
    ): Action[AnyContent] = ???
}

