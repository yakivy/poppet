package poppet.example.play.module

import cats.implicits._
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton
import play.api.Configuration
import play.api.inject.SimpleModule
import play.api.inject._
import play.api.libs.ws.WSClient
import poppet.coder.play.all._
import poppet.consumer.all._
import poppet.example.play.service.UserService
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

class CustomModule extends SimpleModule(
    bind[UserService].toProvider[UserServiceProvider]
)

@Singleton
class UserServiceProvider @Inject()(
    wsClient: WSClient, config: Configuration)(implicit ec: ExecutionContext
) extends Provider[UserService] {
    private val authHeader = config.get[String]("auth.header")
    private val authSecret = config.get[String]("auth.secret")
    private val url = config.get[String]("consumer.url")
    private val client: Client[Future] =
        request => wsClient.url(url).withHttpHeaders(authHeader -> authSecret).post(request)
            .map(_.bodyAsBytes.toByteBuffer.array())

    override def get(): UserService = Consumer[Future].apply(
        client, PlayJsonCoder())(ConsumerProcessor[UserService].generate()
    ).materialize()
}