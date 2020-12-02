package poppet.example.play.service

import cats.implicits._
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton
import play.api.Configuration
import play.api.libs.json.JsValue
import play.api.libs.ws.WSClient
import play.mvc.Http
import poppet.coder.play.all._
import poppet.consumer.all._
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

@Singleton
class UserServiceProvider @Inject()(
    wsClient: WSClient, config: Configuration)(implicit ec: ExecutionContext
) extends Provider[UserService] {
    private val secret = config.get[String]("auth.secret")
    private val url = config.get[String]("consumer.url")

    private val client: Transport[JsValue, Future] =
        request => wsClient.url(url).withHttpHeaders(Http.HeaderNames.PROXY_AUTHENTICATE -> secret)
            .post(request).map(_.body[JsValue])

    override def get: UserService = Consumer[JsValue, Future](client).service[UserService]
}
