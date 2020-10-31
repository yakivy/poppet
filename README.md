## Poppet
[![Maven Central](https://img.shields.io/maven-central/v/com.github.yakivy/poppet-coder-core_2.13.svg)](https://mvnrepository.com/search?q=poppet)
[![Sonatype Nexus (Snapshots)](https://img.shields.io/nexus/s/https/oss.sonatype.org/com.github.yakivy/poppet-coder-core_2.13.svg)](https://oss.sonatype.org/content/repositories/snapshots/com/github/yakivy/poppet-coder-core_2.13/)
[![Build Status](https://travis-ci.com/yakivy/poppet.svg?branch=master)](https://travis-ci.com/yakivy/poppet)
[![codecov.io](https://codecov.io/gh/yakivy/poppet/branch/master/graphs/badge.svg?branch=master)](https://codecov.io/github/yakivy/poppet/branch/master)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
<a href="https://typelevel.org/cats/"><img src="https://typelevel.org/cats/img/cats-badge.svg" height="40px" align="right" alt="Cats friendly" /></a>

Poppet is a minimal, extensible, type-based Scala library for generating RPC services from pure service traits.

### Table of contents
1. [Motivation](#motivation)
1. [Design](#design)
1. [Quick start](#quick-start)
    1. [API](#api)
    1. [Provider](#provider)
    1. [Consumer](#consumer)
1. [Customizations](#customizations)
    1. [Authentication](#authentication)
    1. [Error handling](#error-handling)
1. [Manual calls](#manual-calls)
1. [Examples](#examples)
1. [Notes](#notes)

### Motivation

You may find Poppet useful if you want to...
- build and expose API around service traits with several lines of code
- keep your services sparklingly clean
- customize everything around the library 😄

### Design

Library consists of three main parts: coder, provider and consumer.

`Coder` is responsible for converting low level interaction data type (mainly `Array[Byte]`) into models. Coders on provider and consumer sides should be compatible (generate same intermediate data for same models). OOTB supported coders: `circe`, `play-json`, `jackson`  

`Provider` is responsible for converting consumer requests to service calls, as a materialization result returns request-response function that needs to be exposed for consumer. 

`Consumer` is responsible for proxying calls from service to provider endpoints, as a materialization result returns the fully functional instance of given trait.

### Quick start
Put library version in the build file and add cats dependency, let's assume you are using sbt:
```scala
val poppetVersion = "0.1.0"

libraryDependencies += Seq(
    "org.typelevel" %% "cats-core" % catsVersion
)
```

#### API
Define clean service trait and share it between provider and consumer services:
```scala
case class User(email: String, firstName: String)
trait UserService {
    def findById(id: String): Future[User]
}
```
#### Provider
Add poppet coder, provider and favourite stack dependencies to the build file, as an example I'll take Play Framework with Circe:
```scala
enablePlugins(PlayScala)
libraryDependencies += Seq(
    "io.circe" %% "circe-parser" % circeVersion,
    "io.circe" %% "circe-generic" % circeVersion,
    "com.github.yakivy" %% "poppet-coder-circe" % poppetVersion,
    "com.github.yakivy" %% "poppet-provider" % poppetVersion,
)
```
Implement service trait with actual logic:
```scala
class UserInternalService extends UserService {
    override def findById(id: String): Future[User] = {
        //emulation of business logic
        if (id == "1") Future.successful(User(id, "Antony"))
        else Future.failed(new IllegalArgumentException("User is not found"))
    }
}
```
Create and materialize a provider for the service (can be materialized once and shared for all incoming calls), keep in mind that only abstract methods of the service type will be exposed, that's why you need to explicitly specify trait type:
```scala
import cats.implicits._
import io.circe._
import io.circe.generic.auto._
import poppet.coder.circe.all._
import poppet.provider.all._

implicit val ec: ExecutionContext = ...

val provider = Provider[Json, Future](
    ProviderProcessor[UserService](new UserInternalService).generate()
).materialize()
```
Register the provider:  
```
POST /api/service controller.ProviderController.apply()
```
```scala
@Singleton
class ProviderController @Inject()(
    cc: ControllerComponents)(implicit ec: ExecutionContext
) extends AbstractController(cc) {
    def apply(): Action[ByteString] = Action.async(cc.parsers.byteString)(request =>
        provider(request.body.toByteBuffer.array()).map(Ok(_))
    )
}
```

#### Consumer
Add poppet coder, consumer and favourite stack dependencies to the build file, as a client I'll take Play WS:
```scala
enablePlugins(PlayScala)
libraryDependencies += Seq(
    "io.circe" %% "circe-parser" % circeVersion,
    "io.circe" %% "circe-generic" % circeVersion,
    "com.github.yakivy" %% "poppet-coder-circe" % poppetVersion,
    "com.github.yakivy" %% "poppet-consumer" % poppetVersion,
    ws
)
```
Create and materialize consumer for the service (can be materialized once and shared everywhere):
```scala
import cats.implicits._
import io.circe._
import io.circe.generic.auto._
import poppet.coder.circe.all._
import poppet.consumer.all._

implicit val ec: ExecutionContext = ...

val client: Client[Future] = request => wsClient.url(
    s"http://${providerHostName}/api/service"
).post(request).map(_.bodyAsBytes.toByteBuffer.array())

val userService: UserService = Consumer[Json, Future].apply(
    client)(ConsumerProcessor[UserService].generate()
).materialize()
```
Enjoy 👌
```scala
userService.findById("1")
```

### Customizations
#### Custom kinds
Out of the box library supports only server data kind as a service return kind (`Future` for play, `Id` for spring and so on). To return custom kind in a service you need to define coders (alias for implicit scala `Function1`) from server kind to that kind:
 ```scala
implicit def pureServerCoder[X, Y](implicit coder: Coder[X, Y]): Coder[X, A[Y]]
implicit def pureServiceCoder[X, Y](implicit coder: Coder[X, Y]): Coder[X, B[Y]]
implicit def pureServerLeftCoder[X, Y](implicit coder: Coder[X, B[Y]]): Coder[A[X], B[Y]]
implicit def pureServiceLeftCoder[X, Y](implicit coder: Coder[X, A[Y]]): Coder[B[X], A[Y]]
```
For example, to be able to return `Id` kind from service that is being provided or consumed by play framework, you need coders from `Future` to `Id`:
```scala
// pureServerCoder is already provided in poppet.coder.instances.CoderInstances.coderToFutureCoder
// pureServiceCoder is already provided in poppet.coder.instances.CoderInstances.idCoder
implicit def pureServerLeftCoder[X, Y](implicit coder: Coder[X, Id[Y]]): Coder[Future[X], Id[Y]] =
    a => coder(Await.result(a, Duration.Inf))
// pureServiceLeftCoder is already provided in poppet.coder.instances.CoderInstances.idCoder
```
more examples can be found in `*CoderInstances` traits (for instance `poppet.coder.play.instances.PlayJsonCoderInstances`)

#### Error handling
Development in progress...

#### Custom logic
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

### Manual calls

### Examples
- Http4s with Circe: https://github.com/yakivy/poppet/tree/master/example/http4s
    - run provider: `sbt "; project http4sProviderExample; run"`
    - run consumer: `sbt "; project http4sConsumerExample; run"`
    - put `http://localhost:9002/api/user/1` in the address bar
- Play Framework with Play Json: https://github.com/yakivy/poppet/tree/master/example/play
    - run provider: `sbt "; project playProviderExample; run 9001"`
    - run consumer: `sbt "; project playConsumerExample; run 9002"`
    - put `http://localhost:9002/api/user/1` in the address bar
- And even Spring Framework with Jackson 😲: https://github.com/yakivy/poppet/tree/master/example/spring
    - run provider: `sbt "; project springProviderExample; run"`
    - run consumer: `sbt "; project springConsumerExample; run"`
    - put `http://localhost:9002/api/user/1` in the address bar

### Notes
Library is in active development and initial version is not completed yet.