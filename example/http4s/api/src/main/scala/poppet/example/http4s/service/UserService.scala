package poppet.example.http4s.service

import cats.effect.IO
import poppet.example.http4s.model.User

trait UserService {
    def findById(id: String): IO[User]
}
