package poppet.readme

import scala.concurrent.Future

trait ApiImplementationFixture extends ApiFixture {
    class UserInternalService extends UserService {
        override def findByEmail(email: String): Future[User] = {
            //emulation of business logic
            Future.successful(User(email, "Antony"))
        }
    }
}
