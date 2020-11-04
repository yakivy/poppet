package poppet.example.http4s.consumer

import cats.effect.ExitCode
import cats.effect.IO
import cats.effect.IOApp
import org.http4s.client.blaze._
import org.http4s.implicits._
import org.http4s.server.blaze._
import poppet.example.http4s.consumer.controller.UserController
import poppet.example.http4s.consumer.service.UserServiceProvider
import scala.concurrent.ExecutionContext.global

object Application extends IOApp {
    override def run(args: List[String]): IO[ExitCode] = {
        val secret = "my-secret"
        val clientResource = BlazeClientBuilder[IO](global).resource
        val userService = new UserServiceProvider(secret)(clientResource).get
        val userController = new UserController(userService)
        BlazeServerBuilder[IO](global)
            .bindHttp(9002, "0.0.0.0")
            .withHttpApp(userController.routes.orNotFound)
            .serve
            .compile
            .drain
            .as(ExitCode.Success)
    }
}
