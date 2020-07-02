package poppet.example.service

import poppet.example.model.User
import scala.concurrent.Future

trait UserService {
    def findById(email: String): Future[User]
}
