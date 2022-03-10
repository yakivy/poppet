package poppet.example.http4s.consumer.api

import cats.effect.IO
import io.circe.generic.auto._
import org.http4s._
import org.http4s.circe.CirceEntityCodec._
import org.http4s.dsl.io._
import poppet.example.http4s.service.UserService

class UserApi(userService: UserService) {
    val routes = HttpRoutes.of[IO] {
        case GET -> Root / "api" / "user" / id =>
            userService.findById(id).foldF(InternalServerError(_), Ok(_))
    }
}
