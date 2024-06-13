package poppet.example.http4s.provider

import cats.effect.ExitCode
import cats.effect.IO
import cats.effect.IOApp
import cats.effect.Resource
import org.http4s.blaze.server._
import org.http4s.implicits._
import poppet.example.http4s.provider.api.ProviderApi

object Application extends IOApp {

    override def run(args: List[String]): IO[ExitCode] = (for {
        providerApi <- Resource.pure[IO, ProviderApi](new ProviderApi())
        server <- BlazeServerBuilder[IO]
            .bindHttp(9001, "0.0.0.0")
            .withHttpApp(providerApi.routes.orNotFound)
            .resource
    } yield ExitCode.Success).useForever

}
