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
import poppet.consumer.play.all._
import poppet.example.play.service.ConsumerAuthDecorator
import poppet.example.play.service.UserService
import scala.concurrent.ExecutionContext

class CustomModule extends SimpleModule(
    bind[UserService].toProvider[UserServiceProvider]
)

@Singleton
class UserServiceProvider @Inject()(
    wsClient: WSClient, authDecorator: ConsumerAuthDecorator, config: Configuration)(implicit ec: ExecutionContext
) extends Provider[UserService] {
    private val url = config.get[String]("consumer.url")

    override def get(): UserService = Consumer(
        PlayWsClient(url)(wsClient), List(authDecorator))(
        PlayJsonCoder())(
        ConsumerProcessor[UserService].generate()
    ).materialize()
}