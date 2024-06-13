package poppet.example.tapir.provider.service

import cats.effect.IO
import fs2._
import poppet.example.tapir.model.User
import poppet.example.tapir.service.UserService
import scala.concurrent.duration._

class UserInternalService extends UserService {
    private val users = List("Antony", "John", "Alice")

    override def findByIds(ids: Stream[IO, String]): IO[Stream[IO, User]] = {
        IO.pure(
            ids.zip(Stream.emits(users))
                .map { case (id, name) => User(id, name) }
        )
    }

    override def findById(id: String): IO[User] = IO.delay {
        id.toIntOption.filter(users.isDefinedAt) match {
            case Some(i) => User(i.toString, users(i))
            case _ => throw new IllegalArgumentException("User is not found")
        }
    }

}
