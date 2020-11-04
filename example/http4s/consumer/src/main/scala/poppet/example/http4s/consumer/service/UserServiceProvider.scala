package poppet.example.http4s.consumer.service

import cats.data.EitherT
import cats.effect.ContextShift
import cats.effect.IO
import cats.effect.Resource
import io.circe.Json
import io.circe.generic.auto._
import org.http4s.Status.Successful
import org.http4s._
import org.http4s.client.dsl.io._
import org.http4s.headers._
import org.http4s.implicits._
import poppet.coder.circe.all._
import poppet.consumer.all._
import poppet.example.http4s.model.SR
import poppet.example.http4s.service.UserService
import org.http4s.circe.CirceEntityDecoder._
import org.http4s.circe.CirceEntityEncoder._

class UserServiceProvider(
    secret: String)(
    clientResource: Resource[IO, client.Client[IO]])(
    implicit cs: ContextShift[IO]
) {
    private val client: Client[Json, SR] = request => EitherT(clientResource.use(client =>
        Method.POST.apply(
            request, uri"http://localhost:9001/api/service", Header(Authorization.name.value, secret)
        ).flatMap(client.run(_).use {
            case Successful(response) => response.as[Json].map(Right(_))
            case failedResponse => failedResponse.as[String].map(Left(_))
        })
    ))

    def get: UserService = Consumer[Json, SR](client).service[UserService]
}
