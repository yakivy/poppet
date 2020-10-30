package poppet.example.play.module

import cats.implicits._
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton
import play.api.Configuration
import play.api.inject.SimpleModule
import play.api.inject._
import play.api.libs.json.JsValue
import play.api.libs.ws.WSClient
import play.mvc.Http
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
    private val authSecret = config.get[String]("auth.secret")
    private val url = config.get[String]("consumer.url")
    private val client: Client[Future] =
        request => wsClient.url(url).withHttpHeaders(Http.HeaderNames.PROXY_AUTHENTICATE -> authSecret)
            .post(request).map(_.bodyAsBytes.toByteBuffer.array())

    override def get(): UserService = Consumer[JsValue, Future].apply(
        client)(ConsumerProcessor[UserService].generate()
    ).materialize()
}