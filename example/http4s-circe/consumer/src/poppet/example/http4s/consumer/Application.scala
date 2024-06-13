package poppet.example.http4s.consumer

import cats.effect.ExitCode
import cats.effect.IO
import cats.effect.IOApp
import cats.effect.Resource
import cats.implicits._
import org.http4s.blaze.client._
import org.http4s.blaze.server._
import org.http4s.implicits._
import poppet.example.http4s.consumer.api.UserApi
import poppet.example.http4s.consumer.service.UserServiceProvider

object Application extends IOApp {

    override def run(args: List[String]): IO[ExitCode] = (for {
        config <- Resource.pure[IO, Config](Config(uri"http://localhost:9001/api/service"))
        client <- BlazeClientBuilder[IO].resource
        userService = new UserServiceProvider(config, client).get
        userApi = new UserApi(userService)
        _ <- BlazeServerBuilder[IO]
            .bindHttp(9002, "0.0.0.0")
            .withHttpApp(userApi.routes.orNotFound)
            .resource
    } yield ExitCode.Success).useForever

}
