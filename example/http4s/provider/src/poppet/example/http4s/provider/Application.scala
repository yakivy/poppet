package poppet.example.http4s.provider

import cats.effect.ExitCode
import cats.effect.IO
import cats.effect.IOApp
import org.http4s.blaze.server._
import org.http4s.implicits._
import poppet.example.http4s.provider.api.ProviderApi

object Application extends IOApp {
    override def run(args: List[String]): IO[ExitCode] = {
        val providerApi = new ProviderApi()
        BlazeServerBuilder[IO]
            .bindHttp(9001, "0.0.0.0")
            .withHttpApp(providerApi.routes.orNotFound)
            .serve
            .compile
            .drain
            .as(ExitCode.Success)
    }
}
