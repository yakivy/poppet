package poppet.example.tapir.consumer.api

import cats.effect.IO
import cats.implicits._
import fs2.Stream
import io.circe.generic.auto._
import io.circe.syntax._
import org.http4s.ServerSentEvent
import poppet.example.tapir.model.User
import poppet.example.tapir.service.UserService
import scala.concurrent.duration._
import sttp.capabilities.fs2.Fs2Streams
import sttp.tapir._
import sttp.tapir.generic.auto._
import sttp.tapir.json.circe._

class UserApi(userService: UserService) {

    val routes = List(
        endpoint
            .get
            .in("api" / "user" / path[String]("userId"))
            .out(jsonBody[User])
            .serverLogicSuccess[IO] { userId =>
                userService.findById(userId)
            },
        endpoint
            .get
            .in("api" / "user")
            .out(streamBody(Fs2Streams[IO])(implicitly[Schema[User]], CodecFormat.TextEventStream()))
            .serverLogicSuccess[IO](_ =>
                for {
                    userStream <- userService.findByIds(
                        Stream.emits(List("1", "2", "3")).zipLeft(Stream.awakeEvery[IO](1.second))
                    )
                } yield userStream
                    .map(user => ServerSentEvent(user.asJson.noSpaces.some))
                    .through(ServerSentEvent.encoder[IO])
            )
    )

}
