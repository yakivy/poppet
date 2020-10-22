package poppet.example.play.controller

import akka.util.ByteString
import cats.implicits._
import javax.inject.Inject
import javax.inject.Singleton
import play.api.Configuration
import play.api.mvc.Request
import play.api.mvc._
import poppet.coder.play.all._
import poppet.example.play.service.UserService
import poppet.provider.all._
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

@Singleton
class ProviderController @Inject()(
    config: Configuration, helloService: UserService, cc: ControllerComponents)(
    implicit ec: ExecutionContext
) extends AbstractController(cc) {
    private val authHeader = config.get[String]("auth.header")
    private val authSecret = config.get[String]("auth.secret")

    private val server = Provider[Future].apply(
        PlayJsonCoder())(ProviderProcessor(helloService).generate()
    ).materialize()
    private val authDecorator: Request[ByteString] => Request[ByteString] = request => {
        if (request.headers.get(authHeader).contains(authSecret)) request
        else throw new IllegalArgumentException("Wrong secret!")
    }

    def apply(): Action[ByteString] = Action.async(cc.parsers.byteString)(request =>
        server(authDecorator(request).body.toByteBuffer.array()).map(Ok(_))
    )
}
