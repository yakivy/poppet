package service

import poppet.example.model.User
import poppet.example.service.UserService
import scala.concurrent.Future

class UserInternalService extends UserService {
    override def findById(id: String): Future[User] = {
        //emulation of business logic
        Future.successful(User(id, "Antony"))
    }
}
