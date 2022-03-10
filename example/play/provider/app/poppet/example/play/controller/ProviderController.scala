package poppet.example.play.controller

import cats.implicits._
import javax.inject.Inject
import javax.inject.Singleton
import play.api.libs.json.JsValue
import play.api.mvc._
import poppet.codec.play.all._
import poppet.example.play.service.UserService
import poppet.provider.all._
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

@Singleton
class ProviderController @Inject()(
    userService: UserService, cc: ControllerComponents)(
    implicit ec: ExecutionContext
) extends AbstractController(cc) {
    private val provider = Provider[Future, JsValue]().service(userService)

    def apply(): Action[AnyContent] = Action.async(request =>
        provider(request.body.asJson.get).map(Ok(_))
    )
}
