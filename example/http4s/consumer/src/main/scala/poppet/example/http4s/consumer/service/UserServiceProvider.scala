package poppet.example.http4s.consumer.service

import cats.data.EitherT
import cats.effect.ContextShift
import cats.effect.IO
import io.circe.Json
import io.circe.generic.auto._
import org.http4s.Status.Successful
import org.http4s._
import org.http4s.client.blaze._
import org.http4s.client.dsl.io._
import org.http4s.headers._
import org.http4s.implicits._
import poppet.coder.circe.all._
import poppet.consumer.all._
import poppet.example.http4s.model.SR
import poppet.example.http4s.service.UserService
import org.http4s.circe.CirceEntityDecoder._
import org.http4s.circe.CirceEntityEncoder._
import scala.concurrent.ExecutionContext.global

class UserServiceProvider(authSecret: String)(implicit cs: ContextShift[IO]) {
    private val httpClient = BlazeClientBuilder[IO](global).resource

    private val poppetClient: Client[Json, SR] = request => EitherT(httpClient.use(client =>
        Method.POST.apply(
            request, uri"http://localhost:9001/api/service", Header(Authorization.name.value, authSecret)
        ).flatMap(client.run(_).use {
            case Successful(response) => response.as[Json].map(Right(_))
            case failedResponse => failedResponse.as[String].map(Left(_))
        })
    ))

    def get: UserService = Consumer[Json, SR].apply(
        poppetClient)(ConsumerProcessor[UserService].generate()
    ).materialize()
}
