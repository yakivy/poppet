package poppet.example.tapir.consumer.service

import cats.data.OptionT
import cats.effect.IO
import cats.implicits._
import fs2.Stream
import io.circe.generic.auto._
import io.circe.parser._
import io.circe.syntax._
import io.circe.Json
import org.http4s._
import org.http4s.client.dsl.io._
import poppet.codec.circe.all._
import poppet.consumer.all._
import poppet.consumer.all.Response
import poppet.example.tapir.consumer.Config
import poppet.example.tapir.model.CustomCodecs._
import poppet.example.tapir.model.PoppetArgumentEvent
import poppet.example.tapir.model.PoppetRequestInitEvent
import poppet.example.tapir.model.PoppetResponseInitEvent
import poppet.example.tapir.model.PoppetResultEvent
import poppet.example.tapir.service.UserService
import poppet.example.tapir.Util
import sttp.capabilities.fs2.Fs2Streams
import sttp.client3._
import sttp.model.HeaderNames

class UserServiceProvider(config: Config, client: SttpBackend[IO, Fs2Streams[IO]]) {

    private val transport: Transport[IO, Either[Json, Stream[IO, Json]]] = request =>
        for {
            initEvent <- IO.pure(PoppetRequestInitEvent(
                request.service,
                request.method,
                request.arguments.collect { case (key, Left(json)) => key -> json },
                request.arguments.collect { case (key, Right(_)) => key }.toSet,
            ))
            streamBody = (
                Stream.emit(initEvent.asJson) ++
                    request.arguments.collect { case (key, Right(stream)) =>
                        stream.chunks.map(chunk => PoppetArgumentEvent(key, chunk.toList).asJson)
                    }.toList.parJoinUnbounded
            ).map(data => ServerSentEvent(data.noSpaces.some))
            clientResp <- quickRequest
                .post(uri"${config.consumerUrl}")
                .header(HeaderNames.TransferEncoding, TransferCoding.chunked.coding)
                .streamBody(Fs2Streams[IO])(streamBody.through(ServerSentEvent.encoder))
                .response(asStreamAlwaysUnsafe(Fs2Streams[IO]).map(_.through(ServerSentEvent.decoder)))
                .send(client)
            x <- OptionT(Util.uncons(clientResp.body))
                .subflatMap { case (initRespEvent, restRespEvents) =>
                    initRespEvent.data
                        .flatMap(parse(_).flatMap(_.as[PoppetResponseInitEvent]).toOption)
                        .map(_ -> restRespEvents)
                }
                .getOrElseF(IO.raiseError(new RuntimeException("Init event is not found")))
            (initRespEvent, restRespEvents) = x
            respValue = initRespEvent.eagerResponse match {
                case Some(eagerResponse) => eagerResponse.asLeft[Stream[IO, Json]]
                case _ =>
                    restRespEvents
                        .flatMap(e =>
                            Stream.fromOption(e.data.flatMap(d => parse(d).flatMap(_.as[PoppetResultEvent]).toOption))
                        )
                        .flatMap(e => Stream.emits(e.values))
                        .asRight[Json]
            }
        } yield Response(respValue)

    def get: UserService = Consumer[IO, Either[Json, Stream[IO, Json]]](transport).service[UserService]
}
