package poppet.example.http4s.provider.controller

import cats.effect.IO
import io.circe.Json
import io.circe.generic.auto._
import org.http4s.HttpRoutes
import org.http4s.Request
import org.http4s.dsl.io._
import org.http4s.headers._
import poppet.coder.circe.all._
import poppet.example.http4s.provider.service.UserInternalService
import poppet.example.http4s.service.UserService
import poppet.provider.all._
import scala.concurrent.ExecutionContext.global

class ProviderController(authSecret: String) {
    implicit val cs = IO.contextShift(global)

    val authDecorator: Request[IO] => Request[IO] = request => {
        if (request.headers.get(Authorization).map(_.value).contains(authSecret)) request
        else throw new IllegalArgumentException("Wrong secret")
    }
    val server = Provider[Json, IO].apply(
        ProviderProcessor[UserService](new UserInternalService).generate()
    ).materialize()

    val routes = HttpRoutes.of[IO] {
        case req@POST -> Root / "api" / "service" =>
            authDecorator(req).body.compile.toVector.map(_.toArray).flatMap(server).flatMap(Ok(_))
    }
}
