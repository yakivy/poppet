## Poppet
Poppet is a functional, extensible, type-based Scala library for generating RPC services from pure service traits.

### Table of contents
1. [Quick start](#quick-start)
    1. [Play Framework](#play-framework)
        1. [Provider](#provider)
        1. [Consumer](#consumer)
    1. [Http4s](#http4s)
    1. [Spring Framework](#spring-framework)
1. [Error handling](#error-handling)
1. [Examples](#examples)
1. [Notes](#notes)

### Quick start
Define service API and share it between provider and consumer services (link formatter to the model or implement separately on both sides):
```scala
case class User(email: String, firstName: String)
object User {
    implicit val F = Json.format[User]
}

trait UserService {
    def findById(id: String): Future[User]
}
```
Put library version in the build file:
```scala
val poppetVersion = "0.0.1.2-SNAPSHOT"
```

### Play framework
#### Provider
Implement API on provider side:
```scala
class UserInternalService extends UserService {
    override def findById(id: String): Future[User] = {
        //emulation of business logic
        Future.successful(User(id, "Antony"))
    }
}
```
Add play poppet coder and provider dependencies to the build file, let's assume you are using sbt:
```scala
libraryDependencies += Seq(
    "com.github.yakivy" %% "poppet-coder-play" % poppetVersion,
    "com.github.yakivy" %% "poppet-provider-play" % poppetVersion
)
```
Create a provider for service, keep in mind that only abstract methods of the service type will be exposed, that's why you need to explicitly specify trait type:
```scala
import cats.implicits._
import poppet.provider.play.all._
import poppet.coder.play.all._

def provider(cc: ControllerComponents)(implicit ec: ExecutionContext) = Provider(
    PlayServer(cc), PlayCoder())(
    ProviderProcessor[UserService](new UserInternalService).generate()
)
```
Materialize and register provider:  
**`routes`**
```
POST /api/service controller.ProviderController.apply()
```
**`ProviderController.scala`**  
```scala
import javax.inject.Inject
import play.api.mvc._

@Singleton
class ProviderController @Inject()(
    cc: ControllerComponents)(implicit ec: ExecutionContext
) extends AbstractController(cc) {
    def apply(): Action[ByteString] = provider(cc).materialize()
}
```

#### Consumer
Add play poppet coder and consumer dependencies to the build file:
```scala
libraryDependencies += Seq(
    "com.github.yakivy" %% "poppet-coder-play" % poppetVersion,
    "com.github.yakivy" %% "poppet-consumer-play" % poppetVersion
)
```
Create and materialize consumer for service (can be materialized once and shared everywhere):
```scala
import cats.implicits._
import poppet.coder.play.all._
import poppet.consumer.play.all._

def userService(wsClient: WSClient)(implicit ec: ExecutionContext): UserService = Consumer(
   PlayClient(s"http://${providerHostName}/api/service")(wsClient),
   PlayCoder())(
   ConsumerProcessor[UserService].generate()
).materialize()
```
Enjoy :)
```scala
import javax.inject.Inject
import javax.inject.Singleton
import play.api.mvc._

@Singleton
class UserController @Inject()(
    wsClient: WSClient, cc: ControllerComponents)(
    implicit ec: ExecutionContext
) extends AbstractController(cc) {
    def findById(id: String) = Action.async {
        userService(wsClient).findById(id).map(Ok(_))
    }
}
```

### Http4s
Development in progress...

### Spring framework
Java island in Scala world :)  
Development in progress...

### Error handling
Development in progress...

### Examples
- Play Framework: https://github.com/yakivy/poppet/tree/master/example/play
    - run provider: `sbt "; project playProviderExample; run 9001"`
    - run consumer: `sbt "; project playConsumerExample; run 9002"`
    - put `http://localhost:9002/api/user/1` in the address bar

### Notes
Library is in active development and initial version is not completed yet.