## Poppet
Poppet is a functional, extensible, type-based Scala library for generating RPC services from pure service traits.

### Table of contents
1. [Quick start](#quick-start)
    1. [Play Framework](#play-framework)
        1. [Provider](#provider)
        1. [Consumer](#consumer)
1. [Examples]
1. [Notes](#notes)

### Quick start
Define service API and share it between provider and consumer services:
```scala
case class User(email: String, firstName: String)
trait UserService {
    def findByEmail(email: String): Future[User]
}
```
Implement API on provider side:
```scala
class UserInternalService extends UserService {
    override def findByEmail(email: String): Future[User] = {
        //emulation of business logic
        Future.successful(User(email, "Antony"))
    }
}
```
Put library version in the build file:
```scala
val poppetVersion = "0.0.1.0-SNAPSHOT"
```

### Play framework
#### Provider
Add play poppet provider dependency to the build file, let's assume you are using sbt:
```scala
libraryDependencies += "com.github.yakivy" %% "poppet-provider-play" % poppetVersion
```
Create a provider for service, keep in mind that only abstract methods of the service type will be exposed, that's why you need to explicitly specify trait type:
```scala
import cats.implicits._
import poppet.provider.play.all._
import poppet.coder.play.all._
import play.api.mvc.ControllerComponents
import scala.concurrent.ExecutionContext

def provider(cc: ControllerComponents)(implicit ec: ExecutionContext) = Provider(
    PlayServer(cc), PlayCoder())(
    ProviderProcessor(helloService).generate()
)
```
Materialize and register provider:  
**`routes`**
```
POST /api/user @UserController.apply()
```
**`UserController.scala`**  
```scala
import javax.inject.Inject
import play.api.mvc._

class UserController @Inject()(
    cc: ControllerComponents)(implicit ec: ExecutionContext
) extends AbstractController(cc) {
    def apply() = provider(cc).materialize()
}
```

#### Consumer
Development in progress...

### Examples
- Play Framework: https://github.com/yakivy/poppet/tree/master/example/play
    - run provider: `sbt "; project playProviderExample; run 9001"`

### Notes
Library is in active development and initial version is not completed yet.