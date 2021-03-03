## Poppet
[![Maven Central](https://img.shields.io/maven-central/v/com.github.yakivy/poppet-core_2.13.svg)](https://mvnrepository.com/search?q=poppet)
[![Sonatype Nexus (Snapshots)](https://img.shields.io/nexus/s/https/oss.sonatype.org/com.github.yakivy/poppet-core_2.13.svg)](https://oss.sonatype.org/content/repositories/snapshots/com/github/yakivy/poppet-core_2.13/)
[![Build Status](https://travis-ci.com/yakivy/poppet.svg?branch=master)](https://travis-ci.com/yakivy/poppet)
[![codecov.io](https://codecov.io/gh/yakivy/poppet/branch/master/graphs/badge.svg?branch=master)](https://codecov.io/github/yakivy/poppet/branch/master)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
<a href="https://typelevel.org/cats/"><img src="https://typelevel.org/cats/img/cats-badge.svg" height="40px" align="right" alt="Cats friendly" /></a>

Poppet is a minimal, type-safe RPC Scala library.

Essential differences from [autowire](https://github.com/lihaoyi/autowire):
- no explicit macro application `.call`, result of a consumer is an instance of original trait;
- no hardcoded return kind `Future`, you can specify any monad (has `cats.Monad` typeclass);
- no forced coder dependencies `uPickle`, you can specify any serialization format;
- robust error handling mechanism;
- cleaner macros logic (~50 lines in comparison to ~300).

### Table of contents
1. [Quick start](#quick-start)
1. [Customizations](#customizations)
    1. [Logging](#logging)
    1. [Failure handling](#failure-handling)
    1. [Authentication](#authentication)
1. [Manual calls](#manual-calls)
1. [Examples](#examples)

### Quick start
Put poppet dependency in the build file, as an example I'll take poppet with circe, let's assume you are using SBT:
```scala
val poppetVersion = "0.1.2"

libraryDependencies ++= Seq(
    "com.github.yakivy" %% "poppet-circe" % poppetVersion, //to use circe
    //"com.github.yakivy" %% "poppet-play-json" % poppetVersion, //to use play json
    //"com.github.yakivy" %% "poppet-jackson" % poppetVersion, //to use jackson
    //"com.github.yakivy" %% "poppet-core" % poppetVersion, //to build custom coder
)
```
Define service trait and share it between provider and consumer services:
```scala
case class User(email: String, firstName: String)
trait UserService {
    def findById(id: String): Future[User]
}
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
Create service provider (can be created once and shared for all incoming calls), keep in mind that only abstract methods of the service type will be exposed, that's why you need to explicitly specify trait type:
```scala
import cats.implicits._
import io.circe._
import io.circe.generic.auto._
import poppet.coder.circe.all._
import poppet.provider.all._

implicit val ec: ExecutionContext = ...

val provider = Provider[Json, Future]()
    .service[UserService](new UserInternalService)
    //.service[OtherService](otherService)
```
Create service consumer (can be created once and shared everywhere):
```scala
import cats.implicits._
import io.circe._
import io.circe.generic.auto._
import poppet.coder.circe.all._
import poppet.consumer.all._
import scala.concurrent.ExecutionContext

//change with serious pool
implicit val ec: ExecutionContext = ExecutionContext.global
//change with transport call
val transport: Transport[Json, Future] = request => provider(request)

val userService = Consumer[Json, Future](transport)
    .service[UserService]
```
Enjoy 👌
```scala
userService.findById("1")
```

### Customizations
The library is build on following abstractions:
- `[I]` - is an intermediate data type what your coding framework is working with, can be any serialization format, but it would be easier to choose from existed coder modules, because they come with a bunch of predefined coders;
- `[F[_]]` - is your service data kind, can be any monad (has `cats.Monad` typeclass);
- `poppet.consumer.Transport` - used to transfer the data between consumer and provider, technically it is just the functions from `I` to `I` lifted to passed data kind (`I => F[I]`). So you can use anything as long as it can receive/pass chosen data type;
- `poppet.Coder` - used to code `I` to models and vice versa. It is probably the most complicated technique in the library since it is build on implicits, because of that, poppet comes with a bunch of modules, where you hopefully will find a favourite coder. If it is not there, you can always try to write your own by providing 2 basic implicits like [here](https://github.com/yakivy/poppet/blob/master/circe/src/main/scala/poppet/coder/circe/instances/CirceCoderInstances.scala);
- `poppet.FailureHandler` - used to handle internal failures, more info you can find [here](#failure-handling);
- `poppet.Peek[I, F[_]]` - used to decorate request -> response function without changing the types. Good fit for logging, more info you can find [here](#logging).

#### Logging
Both provider and consumer take `Peek[I, F]` as an argument, that allows to inject logging logic around the `Request[I] => F[Response[I]]` function. Let's define simple logging peek:
```scala
val peek: Peek[Json, Id] = f => request => {
    println("Request: " + request)
    val response = f(request)
    println("Response: " + response)
    response
}
``` 

#### Failure handling
All meaningful failures that can appear in the library are being transformed into `poppet.Failure`, after what, handled with `poppet.FailureHandler`. Failure handler is a simple function from failure to result:
```scala
type FailureHandler[A] = Failure => A
```
by default, throwing failure handler is being resolved:
```scala
implicit def throwingFailureHandler[A]: FailureHandler[A] = throw _
```
so if your don't want to deal with JVM exceptions, you can provide your own instance of failure handler. Let's assume you want to pack a failure with `EitherT[Future, String, A]` kind, then failure handler can look like:
```scala
type SR[A] = EitherT[Future, String, A]
implicit def fh[A]: FailureHandler[SR[A]] = f => EitherT.leftT(f.getMessage)
```
For more info you can check [Http4s with Circe](#examples) example project, it is built around `EitherT[IO, String, A]` kind.

#### Authentication
As the library is abstracted from the transferring protocol, you can inject whatever logic you want around the poppet provider/consumer. For example, you want to add simple authentication for the generated RPC endpoints... Firstly let's add authorization header check on provider side before provider invocation:
```scala
def provide(request: Request): Response = {
    if (!request.headers.get("secret").contains(secret))
        throw new IllegalArgumentException("Wrong secret!")
    provider(request.body[Json]).map(Response(_))
}
```
and then pass authorization header in the consumer transport:
```scala
private val transport: Transport[Future] = request => client
    .url(url)
    .withHttpHeaders("secret" -> secret)
    .post(request)
    .map(_.body[Json])
```
For more info you can check the [examples](#examples), all of them have simple authentication built on the same approach.

### Manual calls
If your coder has a human readable format (JSON for example), you can use a provider without consumer (mostly for debug purposes) by generating requests manually. Here is an example of curl call:
```shell script
curl --location --request POST '${providerUrl}' \
--data-raw '{
    "service": "poppet.UserService", #full class name of the service
    "method": "findById", #method name
    "arguments": {
        "id": "1" #argument name: encoded value
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
