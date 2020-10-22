package poppet.example.http4s.provider

import cats.effect.ExitCode
import cats.effect.IO
import cats.effect.IOApp
import org.http4s.Header
import org.http4s.Headers
import org.http4s.implicits._
import org.http4s.server.blaze._
import poppet.example.http4s.provider.controller.ProviderController
import scala.concurrent.ExecutionContext.global

object Application extends IOApp {
    override def run(args: List[String]): IO[ExitCode] = {
        val authSecret = "my-secret"
        val providerController = new ProviderController(authSecret)
        BlazeServerBuilder[IO](global)
            .bindHttp(9001, "0.0.0.0")
            .withHttpApp(providerController.routes.orNotFound)
            .serve
            .compile
            .drain
            .as(ExitCode.Success)
    }
}
