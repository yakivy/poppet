package poppet.example.tapir.provider

import cats.effect.kernel.Resource
import cats.effect.ExitCode
import cats.effect.IO
import cats.effect.IOApp
import org.http4s.blaze.server._
import org.http4s.implicits._
import poppet.example.tapir.provider.api.ProviderApi
import poppet.example.tapir.provider.service.UserInternalService
import sttp.tapir.server.http4s.Http4sServerInterpreter

object Application extends IOApp {

    override def run(args: List[String]): IO[ExitCode] = (for {
        providerApi <- Resource.pure[IO, ProviderApi](new ProviderApi(new UserInternalService()))
        routes = Http4sServerInterpreter[IO]().toRoutes(providerApi.routes)
        server <- BlazeServerBuilder[IO]
            .bindHttp(9001, "0.0.0.0")
            .withHttpApp(routes.orNotFound)
            .resource
    } yield ExitCode.Success).useForever

}
