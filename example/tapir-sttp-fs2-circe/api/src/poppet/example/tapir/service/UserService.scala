package poppet.example.tapir.service

import cats.effect.IO
import fs2._
import poppet.example.tapir.model.User

trait UserService {
    def findById(id: String): IO[User]
    def findByIds(ids: Stream[IO, String]): IO[Stream[IO, User]]
}
