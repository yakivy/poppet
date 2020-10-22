package poppet.example.http4s.consumer.service

import cats.effect.ContextShift
import cats.effect.IO
import io.circe.generic.auto._
import org.http4s._
import org.http4s.client.blaze._
import org.http4s.client.dsl.io._
import org.http4s.headers._
import org.http4s.implicits._
import poppet.coder.circe.all._
import poppet.consumer.Consumer
import poppet.consumer.core.ConsumerProcessor
import poppet.example.http4s.service.UserService
import scala.concurrent.ExecutionContext.global

class UserServiceProvider(authSecret: String)(implicit cs: ContextShift[IO]) {
    private val client: poppet.consumer.Client[IO] =
        request => BlazeClientBuilder[IO](global).resource.use { client =>
            client.expect[Array[Byte]](Method.POST(
                request,
                uri"http://localhost:9001/api/service",
                Header(Authorization.name.value, authSecret)
            ))
        }

    def get: UserService = Consumer[IO].apply(
        client, CirceCoder())(ConsumerProcessor[UserService].generate()
    ).materialize()
}
