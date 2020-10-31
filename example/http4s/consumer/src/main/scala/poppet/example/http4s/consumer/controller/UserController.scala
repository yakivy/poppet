package poppet.example.http4s.consumer.controller

import cats.effect.IO
import io.circe.generic.auto._
import org.http4s._
import org.http4s.circe.CirceEntityCodec._
import org.http4s.dsl.io._
import poppet.example.http4s.consumer.service.UserServiceProvider

class UserController(userServiceProvider: UserServiceProvider) {
    private val userService = userServiceProvider.get

    val routes = HttpRoutes.of[IO] {
        case GET -> Root / "api" / "user" / id =>
            userService.findById(id).foldF(InternalServerError(_), Ok(_))
    }
}
