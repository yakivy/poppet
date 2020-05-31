package poppet.readme

import scala.concurrent.Future

trait ApiFixture {
    case class User(email: String, firstName: String)
    trait UserService {
        def findByEmail(email: String): Future[User]
    }
}
