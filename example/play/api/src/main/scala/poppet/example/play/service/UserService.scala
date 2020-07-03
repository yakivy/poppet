package poppet.example.play.service

import poppet.example.play.model.User
import scala.concurrent.Future

trait UserService {
    def findById(id: String): Future[User]
}
