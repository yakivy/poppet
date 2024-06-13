package poppet.example.play.service

import cats.implicits._
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton
import play.api.libs.json.JsValue
import play.api.libs.json.Writes
import play.api.libs.ws.WSClient
import play.api.Configuration
import poppet.codec.play.all._
import poppet.consumer.all._
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

@Singleton
class UserServiceProvider @Inject() (
    wsClient: WSClient,
    config: Configuration
)(implicit ec: ExecutionContext) extends Provider[UserService] {
    private val url = config.get[String]("consumer.url")

    private val client: Transport[Future, JsValue] = request =>
        wsClient.url(url).post(Writes.of[Request[JsValue]].writes(request)).map(_.body[JsValue].as[Response[JsValue]])

    override def get: UserService = Consumer[Future, JsValue](client).service[UserService]
}
