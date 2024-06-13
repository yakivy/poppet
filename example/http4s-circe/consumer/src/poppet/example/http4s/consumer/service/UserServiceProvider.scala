package poppet.example.http4s.consumer.service

import cats.data.EitherT
import cats.effect.IO
import io.circe.Json
import io.circe.generic.auto._
import org.http4s.Status.Successful
import org.http4s._
import org.http4s.circe.CirceEntityDecoder._
import org.http4s.circe.CirceEntityEncoder._
import org.http4s.client.Client
import org.http4s.client.dsl.io._
import poppet.codec.circe.all._
import poppet.consumer.all._
import poppet.consumer.all.Response
import poppet.example.http4s.consumer.Config
import poppet.example.http4s.model.SR
import poppet.example.http4s.poppet.SRFailureHandler
import poppet.example.http4s.service.UserService

class UserServiceProvider(config: Config, client: Client[IO]) {
    private val transport: Transport[SR, Json] = request => EitherT(
        client.run(Method.POST.apply(request, config.consumerUrl)).use {
            case Successful(response) => response.as[Response[Json]].map(Right(_))
            case failedResponse => failedResponse.as[String].map(Left(_))
        }
    )

    def get: UserService = Consumer[SR, Json](transport, fh = SRFailureHandler).service[UserService]
}
