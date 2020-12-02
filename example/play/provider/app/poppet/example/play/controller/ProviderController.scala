package poppet.example.play.controller

import cats.implicits._
import javax.inject.Inject
import javax.inject.Singleton
import play.api.Configuration
import play.api.libs.json.JsValue
import play.api.mvc._
import play.mvc.Http
import poppet.coder.play.all._
import poppet.example.play.service.UserService
import poppet.provider.all._
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

@Singleton
class ProviderController @Inject()(
    config: Configuration, userService: UserService, cc: ControllerComponents)(
    implicit ec: ExecutionContext
) extends AbstractController(cc) {
    private val authSecret = config.get[String]("auth.secret")

    private def checkAuth(request: Request[AnyContent]): Request[AnyContent] = {
        if (request.headers.get(Http.HeaderNames.PROXY_AUTHENTICATE).contains(authSecret)) request
        else throw new IllegalArgumentException("Wrong secret!")
    }

    private val provider = Provider[JsValue, Future]().service(userService)

    def apply(): Action[AnyContent] = Action.async(request =>
        provider(checkAuth(request).body.asJson.get).map(Ok(_))
    )
}
