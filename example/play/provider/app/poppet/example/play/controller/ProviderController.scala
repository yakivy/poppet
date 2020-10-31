package poppet.example.play.controller

import akka.util.ByteString
import cats.Id
import cats.implicits._
import javax.inject.Inject
import javax.inject.Singleton
import play.api.Configuration
import play.api.libs.json.JsPath
import play.api.libs.json.JsValue
import play.api.libs.json.JsonValidationError
import play.api.mvc.Request
import play.api.mvc._
import play.mvc.Http
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
    private val authSecret = config.get[String]("auth.secret")

    private val authDecorator: Request[ByteString] => Request[ByteString] = request => {
        if (request.headers.get(Http.HeaderNames.PROXY_AUTHENTICATE).contains(authSecret)) request
        else throw new IllegalArgumentException("Wrong secret!")
    }
    private val provider = Provider[JsValue, Future].apply(
        ProviderProcessor(helloService).generate()
    ).materialize()

    def apply(): Action[ByteString] = Action.async(cc.parsers.byteString)(request =>
        provider(authDecorator(request).body.toByteBuffer.array()).map(Ok(_))
    )
}
