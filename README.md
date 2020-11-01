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
    1. [Failure handling](#failure-handling)
1. [Manual calls](#manual-calls)
1. [Examples](#examples)

### Motivation

You may find Poppet useful if you want to...
- automate an RPC services generation from the service traits with several lines of code
- keep your services sparkling clean
- customize almost every piece of the library you are using 😄

### Design

Library consists of three main parts: coder, provider and consumer.

`Coder` is responsible for converting low level interaction data type (`Array[Byte]`) into the models. Coders on the provider and consumer sides should be compatible (generate same interaction data for same models). Out of the box coders: `circe`, `play-json`, `jackson`  

`Provider` is responsible for converting consumer requests to service calls, as a materialization result returns request-response function that needs to be exposed for the consumer. 

`Consumer` is responsible for proxying calls from service to provider endpoints, as a materialization result returns the fully functioning instance of the given trait.

### Quick start
Put a library version in the build file and add cats dependency, let's assume you are using SBT:
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
class ProviderController(
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
val wsClient: WSClient = ...

val client: Client[Future] = request => wsClient.url(
    s"http://${providerHostName}/api/service"
).post(request).map(_.bodyAsBytes.toByteBuffer.array())

val userService: UserService = Consumer[Json, Future](
    client)(ConsumerProcessor[UserService].generate()
).materialize()
```
Enjoy 👌
```scala
userService.findById("1")
```

### Customizations
The library is build on following abstractions:
- `[I, F[_]]` - probably the first two letters that you saw in the poppet. `I` - is an intermediate data type what your coding framework is working with, can be any serialization format, but it would be easier to choose from [here](https://github.com/yakivy/poppet/tree/master/coder), because they come with a bunch of predefined coders. `F` - is your service data kind, can be any monad (has `cats.Monad` typeclass);
- `poppet.provider.Server`/`poppet.consumer.Client` - used for data transferring, technically they are just the functions from bytes to bytes lifted to passed data kind (`Array[Byte] => F[Array[Byte]]`). So you can use anything as long as it can receive/pass an array of bytes (for more info you can check the [examples](#examples), all of them were build on different web frameworks) and decorate it as you wish (example with authentication is [here](#authentication));
- `poppet.ExchangeCoder`/`poppet.ModelCoder` - used for coding bytes to intermediate format/intermediate format to models. It is probably the most complicated technique in the library since it is build on implicits, because of that, poppet comes with a bunch of `poppet-coder-*` modules, where you hopefully will find a favourite coder. If it is not there, you can always try to write your own by providing 4 basic implicits like in `poppet.coder.circe.instances.CirceCoderInstances`;
- `poppet.FailureHandler` - used for handling failures, more info you can find [here](#failure-handling).

#### Authentication
As the library is abstracted from the transferring protocol, you can inject whatever logic you want around the poppet provider/consumer. For example, you want to add simple authentication for the generated RPC endpoints... Firstly let's write the method that will check `Authorization` header from the request on provider side, as an example I'll take Play Framework:
```
private def checkAuth(request: Request[ByteString]): Request[ByteString] = {
    if (request.headers.get(Http.HeaderNames.PROXY_AUTHENTICATE).contains(authSecret)) request
    else throw new IllegalArgumentException("Wrong secret!")
}
```
and integrate it into the poppet endpoint like:
```
def apply(): Action[ByteString] = Action.async(cc.parsers.byteString)(request =>
    provider(checkAuth(request).body.toByteBuffer.array()).map(Ok(_))
)
```
so the original goal is already reached, the only thing that left is to pass `Authorisation` header from the consumer. To this end, you can easily modify the consumer client:
```
private val client: Client[Future] = request => wsClient.url(url)
    .withHttpHeaders(Http.HeaderNames.PROXY_AUTHENTICATE -> authSecret)
    .post(request).map(_.bodyAsBytes.toByteBuffer.array())
```
For more info you can check the [examples](#examples), all of them have simple authentication build on the same approach.

#### Failure handling
All meaningful failures that can appear in the library are being transformed into `poppet.Failure`, after what, handled with `poppet.FailureHandler`. Failure handler is a simple function from failure to result:
```
type FailureHandler[A] = Failure => A
```
by default, throwing failure handler is being resolved:
```
implicit def throwingFailureHandler[A]: FailureHandler[A] = throw _
```
so if your don't want to deal with JVM exceptions, you can provide your own instance of failure handler. Let's assume you want to pack a failure with `EitherT[Future, String, A]` kind, then failure handler can look like:
```
type SR[A] = EitherT[Future, String, A]
implicit def fh[A]: FailureHandler[SR[A]] = a => EitherT.leftT(a.getMessage)
```
For more info you can check [Http4s with Circe](#examples) example project, it is build around `EitherT[IO, String, A]` kind.

### Manual calls
You also can to use a provider without consumer (mostly for debug purposes) by generating requests manually. Here is an example of request body for json-like coder:
```
{
    "service": "poppet.UserService", //full class name of the service
    "method": "findById", //method name
    "arguments": {
        "id": "1" //argument name: encoded value
    }
}
```
so cURL call will look like:
```
curl --location --request POST 'http://${providerHostName}/api/service' \
--data-raw '{
    "service": "poppet.UserService",
    "method": "findById",
    "arguments": {
        "id": "1"
    }
}'
```

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
