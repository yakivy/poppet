package poppet.example.http4s.provider.service

import cats.data.EitherT
import poppet.example.http4s.model.SR
import poppet.example.http4s.model.User
import poppet.example.http4s.service.UserService

class UserInternalService extends UserService {
    override def findById(id: String): SR[User] = {
        //emulation of business logic
        if (id == "1") EitherT.rightT(User(id, "Antony"))
        else EitherT.leftT("User is not found")
    }
}
