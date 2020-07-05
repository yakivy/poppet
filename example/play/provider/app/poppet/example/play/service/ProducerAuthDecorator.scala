package poppet.example.play.service

import akka.util.ByteString
import javax.inject.Inject
import javax.inject.Singleton
import play.api.Configuration
import play.api.mvc.Request
import play.api.mvc.Result
import poppet.Decorator
import scala.concurrent.Future

@Singleton
class ProducerAuthDecorator @Inject()(config: Configuration)
    extends Decorator[Request[ByteString], Result, Future] {
    private val authHeader = config.get[String]("auth.header")
    private val authSecret = config.get[String]("auth.secret")

    override def apply(chain: Request[ByteString] => Future[Result]): Request[ByteString] => Future[Result] =
        ((rq: Request[ByteString]) => {
            if (!rq.headers.get(authHeader).contains(authSecret))
                throw new IllegalArgumentException("Wrong secret!")
            else rq
        }).andThen(chain)
}
