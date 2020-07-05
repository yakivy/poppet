package poppet.example.play.service

import javax.inject.Inject
import javax.inject.Singleton
import play.api.Configuration
import play.api.libs.ws.WSRequest
import play.api.libs.ws.WSResponse
import poppet.Decorator
import scala.concurrent.Future

@Singleton
class ConsumerAuthDecorator @Inject()(config: Configuration) extends Decorator[WSRequest, WSResponse, Future] {
    private val authHeader = config.get[String]("auth.header")
    private val authSecret = config.get[String]("auth.secret")

    override def apply(chain: WSRequest => Future[WSResponse]): WSRequest => Future[WSResponse] =
        ((_: WSRequest).addHttpHeaders(authHeader -> authSecret)).andThen(chain)
}
