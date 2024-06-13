package poppet.example.tapir.provider.api

import cats.data.OptionT
import cats.effect.std.Queue
import cats.effect.IO
import cats.implicits._
import fs2.concurrent.Channel
import fs2.Stream
import io.circe.generic.auto._
import io.circe.parser._
import io.circe.syntax._
import io.circe.Json
import java.time.Instant
import org.http4s.ServerSentEvent
import poppet.codec.circe.all._
import poppet.example.tapir.model.CustomCodecs._
import poppet.example.tapir.model.PoppetArgumentEvent
import poppet.example.tapir.model.PoppetRequestInitEvent
import poppet.example.tapir.model.PoppetResponseInitEvent
import poppet.example.tapir.model.PoppetResultEvent
import poppet.example.tapir.model.User
import poppet.example.tapir.service.UserService
import poppet.example.tapir.Util
import poppet.provider.all._
import poppet.Request
import sttp.capabilities.fs2.Fs2Streams
import sttp.tapir._
import sttp.tapir.json.circe._

class ProviderApi(userService: UserService) {
    private val provider = Provider[IO, Either[Json, Stream[IO, Json]]]().service(userService)

    val routes = List(
        endpoint
            .post
            .in("api" / "service")
            .in(streamBody(Fs2Streams[IO])(implicitly[Schema[Json]], CodecFormat.OctetStream()))
            .out(streamBody(Fs2Streams[IO])(implicitly[Schema[Json]], CodecFormat.TextEventStream()))
            .serverLogicSuccess[IO] { case (inputStream) =>
                for {
                    eventsStream <- IO.pure(
                        inputStream
                            .through(ServerSentEvent.decoder)
                            .flatMap(e => Stream.fromOption(e.data.flatMap(parse(_).toOption)))
                    )
                    x <- OptionT
                        .apply(Util.uncons(eventsStream))
                        .subflatMap { case (initReqEvent, restReqEvents) =>
                            initReqEvent.as[PoppetRequestInitEvent].toOption.map(_ -> restReqEvents)
                        }
                        .getOrElseF(IO.raiseError(new RuntimeException("Init event is not found")))
                    (initReqEvent, restReqEvents) = x
                    resultStream = for {
                        channels <- Stream.eval(initReqEvent.streamArguments.toList.traverse(arg =>
                            Channel.synchronous[IO, Json].map(arg -> _)
                        ).map(_.toMap))
                        _ <- Stream.resource((for {
                            _ <- restReqEvents
                                .flatMap(e => Stream.fromOption(e.as[PoppetArgumentEvent].toOption))
                                .evalMap(e => e.values.traverse(channels(e.argumentName).send))
                                .compile
                                .drain
                            _ <- channels.values.toList.traverse(_.close)
                        } yield ()).background.void)
                        providerResult <- Stream.eval(provider(Request(
                            initReqEvent.service,
                            initReqEvent.method,
                            initReqEvent.eagerArguments.view.mapValues(_.asLeft[Stream[IO, Json]]).toMap ++
                                initReqEvent.streamArguments.map(arg => arg -> channels(arg).stream.asRight[Json])
                        )))
                        resultEvent <- providerResult.value.fold(
                            json => Stream.emit(PoppetResponseInitEvent(json.some).asJson),
                            stream =>
                                Stream.emit(PoppetResponseInitEvent(None).asJson) ++
                                    stream.chunks.map(chunk => PoppetResultEvent(chunk.toList).asJson),
                        )
                    } yield resultEvent
                } yield resultStream
                    .map(c => ServerSentEvent(data = c.noSpaces.some))
                    .through(ServerSentEvent.encoder)
            }
    )

}
