package poppet.example.http4s.provider.api

import cats.data.EitherT
import cats.effect.IO
import io.circe.Json
import io.circe.generic.auto._
import org.http4s.HttpRoutes
import org.http4s.circe.CirceEntityDecoder._
import org.http4s.circe.CirceEntityEncoder._
import org.http4s.dsl.io._
import poppet.codec.circe.all._
import poppet.example.http4s.model.SR
import poppet.example.http4s.poppet.SRFailureHandler
import poppet.example.http4s.provider.service.UserInternalService
import poppet.example.http4s.service.UserService
import poppet.provider.all._

class ProviderApi() {
    val provider = Provider[SR, Json](fh = SRFailureHandler).service[UserService](new UserInternalService)

    val routes = HttpRoutes.of[IO] {
        case request@POST -> Root / "api" / "service" => (for {
            byteBody <- EitherT.right[String](request.as[Json])
            response <- provider(byteBody)
        } yield response).foldF(InternalServerError(_), Ok(_))
    }
}
