package module

import cats.implicits._
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton
import play.api.inject.SimpleModule
import play.api.inject._
import play.api.libs.ws.WSClient
import poppet.coder.play.all._
import poppet.consumer.play.all._
import poppet.example.service.UserService
import scala.concurrent.ExecutionContext

class CustomModule extends SimpleModule(
    bind[UserService].toProvider[UserServiceProvider]
)

@Singleton
class UserServiceProvider @Inject()(
    wsClient: WSClient)(implicit ec: ExecutionContext
) extends Provider[UserService] {
    override def get(): UserService = Consumer(
        PlayClient("http://localhost:9001/api/service")(wsClient),
        PlayCoder())(
        ConsumerProcessor[UserService].generate()
    ).materialize()
}