package poppet.example.http4s.consumer

import cats.effect.ExitCode
import cats.effect.IO
import cats.effect.IOApp
import org.http4s.blaze.client._
import org.http4s.blaze.server._
import org.http4s.implicits._
import poppet.example.http4s.consumer.api.UserApi
import poppet.example.http4s.consumer.service.UserServiceProvider

object Application extends IOApp {
    override def run(args: List[String]): IO[ExitCode] = {
        val config = Config(uri"http://localhost:9001/api/service")
        val clientResource = BlazeClientBuilder[IO].resource
        val userService = new UserServiceProvider(config, clientResource).get
        val userApi = new UserApi(userService)
        BlazeServerBuilder[IO]
            .bindHttp(9002, "0.0.0.0")
            .withHttpApp(userApi.routes.orNotFound)
            .serve
            .compile
            .drain
            .as(ExitCode.Success)
    }
}
