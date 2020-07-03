package poppet.example.play.controller

import akka.util.ByteString
import cats.implicits._
import javax.inject.Inject
import javax.inject.Singleton
import play.api.mvc._
import poppet.coder.play.all._
import poppet.example.play.service.UserService
import poppet.provider.play.all._
import scala.concurrent.ExecutionContext

@Singleton
class ProviderController @Inject()(
    helloService: UserService, cc: ControllerComponents)(
    implicit ec: ExecutionContext
) extends AbstractController(cc) {
    def apply(): Action[ByteString] = Provider(
        PlayServer(cc),
        PlayCoder())(
        ProviderProcessor(helloService).generate()
    ).materialize()
}
