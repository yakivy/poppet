package poppet.example.http4s.provider.controller

import cats.data.EitherT
import cats.effect.IO
import io.circe.Json
import io.circe.generic.auto._
import org.http4s.HttpRoutes
import org.http4s.Request
import org.http4s.dsl.io._
import org.http4s.headers._
import poppet.example.http4s.model.SR
import poppet.example.http4s.provider.service.UserInternalService
import poppet.example.http4s.service.UserService
import poppet.example.http4s.poppet.coder.all._
import poppet.provider.all._
import scala.concurrent.ExecutionContext.global

class ProviderController(authSecret: String) {
    implicit val cs = IO.contextShift(global)

    val server = Provider[Json, SR].apply(
        ProviderProcessor[UserService](new UserInternalService).generate()
    ).materialize()

    val routes = HttpRoutes.of[IO] {
        case request@POST -> Root / "api" / "service" => (for {
            _ <- checkAuth(request)
            byteBody <- EitherT.right[String](request.body.compile.toVector.map(_.toArray))
            poppetResponse <- server(byteBody)
        } yield poppetResponse).foldF(InternalServerError(_), Ok(_))
    }

    def checkAuth(request: Request[IO]): SR[Unit] = {
        if (request.headers.get(Authorization.name).map(_.value).contains(authSecret)) EitherT.rightT(request)
        else EitherT.leftT("Wrong secret")
    }
}
