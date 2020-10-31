package poppet.example.play.service

import poppet.example.play.model.User
import poppet.example.play.service.UserService
import scala.concurrent.Future

class UserInternalService extends UserService {
    override def findById(id: String): Future[User] = {
        //emulation of business logic
        if (id == "1") Future.successful(User(id, "Antony"))
        else Future.failed(new IllegalArgumentException("User is not found"))
    }
}
