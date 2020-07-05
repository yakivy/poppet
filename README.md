## Poppet
[![Maven Central](https://img.shields.io/maven-central/v/com.github.yakivy/poppet-coder-core_2.13.svg)](https://mvnrepository.com/search?q=poppet)
[![Sonatype Nexus (Snapshots)](https://img.shields.io/nexus/s/https/oss.sonatype.org/com.github.yakivy/poppet-coder-core_2.13.svg)](https://oss.sonatype.org/content/repositories/snapshots/com/github/yakivy/poppet-coder-core_2.13/)
[![Build Status](https://travis-ci.com/yakivy/poppet.svg?branch=master)](https://travis-ci.com/yakivy/poppet)
[![codecov.io](https://codecov.io/gh/yakivy/poppet/branch/master/graphs/badge.svg?branch=master)](https://codecov.io/github/yakivy/poppet/branch/master)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
<a href="https://typelevel.org/cats/"><img src="https://typelevel.org/cats/img/cats-badge.svg" height="40px" align="right" alt="Cats friendly" /></a>

Poppet is a functional, extensible, type-based Scala library for generating RPC services from pure service traits.

### Table of contents
1. [Quick start](#quick-start)
    1. [Play Framework](#play-framework)
        1. [API](#api)
        1. [Provider](#provider)
        1. [Consumer](#consumer)
    1. [Http4s](#http4s)
    1. [Spring Framework](#spring-framework)
        1. [API](#api-1)
        1. [Provider](#provider-1)
        1. [Consumer](#consumer-1)
1. [Decorators](#decorators)
1. [Custom kinds](#custom-kinds)
1. [Error handling](#error-handling)
1. [Examples](#examples)
1. [Notes](#notes)

### Quick start
Put library version in the build file and add cats dependency, let's assume you are using sbt:
```scala
val poppetVersion = "0.0.1"

libraryDependencies += Seq(
    "org.typelevel" %% "cats-core" % catsVersion
)
```

### Play framework
#### API
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
Add play poppet coder and provider dependencies to the build file
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
    PlayServer(cc))(
    PlayCoder())(
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
@Singleton
class ProviderController @Inject()(
    cc: ControllerComponents)(implicit ec: ExecutionContext
) extends AbstractController(cc) {
    def apply(): Action[ByteString] = provider(cc).materialize()
}
```

#### Consumer
Add play coder and consumer dependencies to the build file, as play consumer is built on play WsClient we will also need it:
```scala
libraryDependencies += Seq(
    "com.github.yakivy" %% "poppet-coder-play" % poppetVersion,
    "com.github.yakivy" %% "poppet-consumer-play" % poppetVersion,
    ws
)
```
Create and materialize consumer for service (can be materialized once and shared everywhere):
```scala
import cats.implicits._
import poppet.coder.play.all._
import poppet.consumer.play.all._

def userService(wsClient: WSClient)(implicit ec: ExecutionContext): UserService = Consumer(
   PlayClient(s"http://${providerHostName}/api/service")(wsClient))(
   PlayCoder())(
   ConsumerProcessor[UserService].generate()
).materialize()
```
Enjoy :)
```scala
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
If you don't hesitate to put scala classes in your java project then you can freely use this library for java interfaces.

#### API
Define service API and share it between provider and consumer services:
```java
public class User {
    private String email;
    private String firstName;

    public User() {}

    public User(String email, String firstName) {
        this.email = email;
        this.firstName = firstName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }
}

public interface UserService {
    public User findById(String id);
}
```

#### Provider
Implement API on provider side:
```scala
@Service
public class UserInternalService implements UserService {
    @Override
    public User findById(String id) {
        return new User(id, "Antony");
    }
}
```
Add spring coder and provider dependencies to the build file, as jackson coder doesn't work with scala classes out of the box we will also need to include jackson scala module:
```scala
libraryDependencies += Seq(
    "com.github.yakivy" %% "poppet-coder-jackson" % poppetVersion,
    "com.github.yakivy" %% "poppet-provider-spring" % poppetVersion,
    "com.fasterxml.jackson.module" %% "jackson-module-scala" % jacksonVersion
)
```
Create separate scala provider generator, keep in mind that only abstract methods of the service type will be exposed:
```scala
import poppet.coder.jackson.all._
import poppet.provider.spring.all._

object ProviderGenerator {
    def apply(
        userService: UserService
    ): RequestEntity[Array[Byte]] => ResponseEntity[Array[Byte]] = Provider(
        SpringServer())(
        JacksonCoder())(
        ProviderProcessor(userService).generate()
    ).materialize()
}
```
Register provider:  
```java
@Controller
public class ProviderController {
    private UserService userService;

    public ProviderController(UserService userService) {
        this.userService = userService;
    }

    @RequestMapping("/api/service")
    public ResponseEntity<byte[]> apply(RequestEntity<byte[]> request) {
        return ProviderGenerator.apply(userService).apply(request);
    }
}
```

#### Consumer
Add spring coder and consumer dependencies to the build file:
```scala
libraryDependencies += Seq(
    "com.github.yakivy" %% "poppet-coder-jackson" % poppetVersion,
    "com.github.yakivy" %% "poppet-consumer-spring" % poppetVersion,
    "com.fasterxml.jackson.module" %% "jackson-module-scala" % jacksonVersion
)
```
Create separate scala consumer generator (can be materialized once and shared everywhere):
```scala
import poppet.coder.jackson.all._
import poppet.consumer.spring.all._

object ConsumerGenerator {
    def userService(restTemplate: RestTemplate): UserService = Consumer(
        SpringClient(s"http://${providerHostName}:9001/api/service")(restTemplate))(
        JacksonCoder())(
        ConsumerProcessor[UserService].generate()
    ).materialize()
}
```
Enjoy :)
```java
@RestController
public class UserController {
    private UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @RequestMapping("/api/user/{id}")
    public User findById(@PathVariable("id") String id) {
        return userService.findById(id);
    }
}
```

### Decorators
Let's say you want to add simple auth for generated RPC endpoints, you can easily do it with help of decorators. Decorator is an alias for scala function that receives poppet flow as an input and returns decorated flow of the same type.  
Firstly we need to define header where we want to pass the secret and the secret by itself:
```scala
val authHeader = "auth"
val authSecret = "my-secret"
```
Then we can create client decorator that will add auth header in all client requests (example is for `play-ws` consumer):
```scala
val consumerAuthDecorator = new Decorator[WSRequest, WSResponse, Future] {
    override def apply(chain: WSRequest => Future[WSResponse]): WSRequest => Future[WSResponse] =
        ((_: WSRequest).addHttpHeaders(authHeader -> authSecret)).andThen(chain)
}
```
register it in the consumer:
```scala
Consumer(
    PlayWsClient(providerUrl)(wsClient), List(authDecorator))(
    PlayJsonCoder())(
    ConsumerProcessor[UserService].generate()
).materialize()
```
And finally check if auth header is present in all server requests with server decorator:
```scala
val producerAuthDecorator = new Decorator[Request[ByteString], Result, Future] {
    override def apply(chain: Request[ByteString] => Future[Result]): Request[ByteString] => Future[Result] =
        ((rq: Request[ByteString]) => {
            if (!rq.headers.get(authHeader).contains(authSecret))
                throw new IllegalArgumentException("Wrong secret!")
            else rq
        }).andThen(chain)
}
```
```scala
Provider(
    PlayServer(cc), List(producerAuthDecorator))(
    PlayJsonCoder())(
    ProviderProcessor(helloService).generate()
).materialize()
```

### Custom kinds
Out of the box library supports only server data kind as a service return kind (`Future` for play, `Id` for spring and so on). To return custom kind in a service you need to define coders (alias for implicit scala `Function1`) from server kind to that kind. For example, to be able to return `Id` kind from service that is being provided or consumed by play framework, you need coders from `Future` to `Id`:
```scala
implicit def futureCoderToIdCoder[A, B](
    implicit coder: Coder[A, Future[B]]
): Coder[A, B] = a => Await.result(coder(a), Duration.Inf)
implicit def coderToLeftFutureCoder[A, B](
    implicit coder: Coder[A, B]
): Coder[Future[A], B] = a => coder(Await.result(a, Duration.Inf))
```
more examples can be found in `*CoderInstances` traits (for instance `poppet.coder.play.instances.PlayJsonCoderInstances`)

### Error handling
Development in progress...

### Examples
- Play Framework: https://github.com/yakivy/poppet/tree/master/example/play
    - run provider: `sbt "; project playProviderExample; run 9001"`
    - run consumer: `sbt "; project playConsumerExample; run 9002"`
    - put `http://localhost:9002/api/user/1` in the address bar
- Spring Framework: https://github.com/yakivy/poppet/tree/master/example/spring
    - run provider: `sbt "; project springProviderExample; run"`
    - run consumer: `sbt "; project springConsumerExample; run"`
    - put `http://localhost:9002/api/user/1` in the address bar

### Notes
Library is in active development and initial version is not completed yet.