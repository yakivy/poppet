package poppet.example.play.controller

import javax.inject.Inject
import javax.inject.Singleton
import play.api.libs.json.Json
import play.api.mvc._
import poppet.example.play.service.UserService
import scala.concurrent.ExecutionContext

@Singleton
class UserController @Inject()(
    userService: UserService, cc: ControllerComponents)(
    implicit ec: ExecutionContext
) extends AbstractController(cc) {
    def findById(id: String) = Action.async {
        userService.findById(id).map(user => Ok(Json.toJson(user)))
    }
}
