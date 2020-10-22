package poppet.example.http4s.provider.service

import cats.effect.IO
import poppet.example.http4s.model.User
import poppet.example.http4s.service.UserService

class UserInternalService extends UserService {
    override def findById(id: String): IO[User] = {
        //emulation of business logic
        IO.pure(User(id, "Antony"))
    }
}
