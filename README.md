## Poppet
[![Maven Central](https://img.shields.io/maven-central/v/com.github.yakivy/poppet-core_2.13.svg)](https://mvnrepository.com/search?q=poppet)
[![Sonatype Nexus (Snapshots)](https://img.shields.io/nexus/s/https/oss.sonatype.org/com.github.yakivy/poppet-core_2.13.svg)](https://oss.sonatype.org/content/repositories/snapshots/com/github/yakivy/poppet-core_2.13/)
[![Build Status](https://travis-ci.com/yakivy/poppet.svg?branch=master)](https://travis-ci.com/yakivy/poppet)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
<a href="https://typelevel.org/cats/"><img src="https://typelevel.org/cats/img/cats-badge.svg" height="40px" align="right" alt="Cats friendly" /></a>

Poppet is a minimal, type-safe RPC Scala library.

Essential differences from [autowire](https://github.com/lihaoyi/autowire):
- no explicit macro application `.call`, result of a consumer is an instance of original trait
- no restricted kind `Future`, you can specify any monad (has `cats.Monad` typeclass) as a processor kind, and an arbitrary kind for trait methods
- no forced codec dependencies `uPickle`, you can choose from predefined codecs or simply implement your own
- robust failure handling mechanism
- has a foundation for Scala 3 support (currently is blocked by https://github.com/lampepfl/dotty/pull/14124)

### Table of contents
1. [Quick start](#quick-start)
1. [Customizations](#customizations)
    1. [Logging](#logging)
    1. [Failure handling](#failure-handling)
1. [Manual calls](#manual-calls)
1. [Examples](#examples)
1. [Changelog](#changelog)

### Quick start
Put cats and poppet dependencies in the build file, let's assume you are using SBT:
```scala
val version = new {
    cats = "2.6.1"
    poppet = "0.2.0"
}

libraryDependencies ++= Seq(
    "org.typelevel" %% "cats-core" % version.cats,
    "com.github.yakivy" %% "poppet-circe" % version.poppet, //to use circe
    //"com.github.yakivy" %% "poppet-play-json" % version.poppet, //to use play json
    //"com.github.yakivy" %% "poppet-jackson" % version.poppet, //to use jackson
    //"com.github.yakivy" %% "poppet-core" % version.poppet, //to build custom codec
)
```
Define service trait and share it between provider and consumer apps:
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
Create service provider (can be created once and shared for all incoming calls), keep in mind that only abstract methods of the service type will be exposed, so you need to explicitly specify a trait type:
```scala
import cats.implicits._
import io.circe._
import io.circe.generic.auto._
import poppet.codec.circe.all._
import poppet.provider.all._

//replace with serious pool
implicit val ec: ExecutionContext = ExecutionContext.global

val provider = Provider[Future, Json]()
    .service[UserService](new UserInternalService)
    //.service[OtherService](otherService)
```
Create service consumer (can be created once and shared everywhere):
```scala
import cats.implicits._
import io.circe._
import io.circe.generic.auto._
import poppet.codec.circe.all._
import poppet.consumer.all._
import scala.concurrent.ExecutionContext

//replace with serious pool
implicit val ec: ExecutionContext = ExecutionContext.global
//replace with actual transport call
val transport: Transport[Future, Json] = request => provider(request)

val userService = Consumer[Future, Json](transport)
    .service[UserService]
```
Enjoy 👌
```scala
userService.findById("1")
```

### Customizations
The library is build on following abstractions:
- `[F[_]]` - is your service data kind, can be any monad (has `cats.Monad` typeclass);
- `[I]` - is an intermediate data type that your coding framework works with, can be any serialization format, but it would be easier to choose from existed codec modules as they come with a bunch of predefined codecs;
- `poppet.consumer.Transport` - used to transfer the data between consumer and provider apps, technically it is just a function from `[I]` to `[F[I]]`, so you can use anything as long as it can receive/pass the chosen data type;
- `poppet.Codec` - used to convert `[I]` to domain models and vice versa. Poppet comes with a bunch of modules, where you will hopefully find a favourite codec. If it is not there, you can always try to write your own by providing 2 basic implicits like [here](https://github.com/yakivy/poppet/blob/master/circe/src/poppet/codec/circe/instances/CirceCodecInstances.scala);
- `poppet.CodecK` - used to convert method return kind to `[F]` and vice versa. It's needed only if return kind differs from your service kind, compilation errors will hint you what codecs are absent;
- `poppet.FailureHandler[F[_]]` - used to handle internal failures, more info you can find [here](#failure-handling);
- `poppet.Peek[F[_], I]` - used to decorate request -> response function. Good fit for logging, more info you can find [here](#logging).

#### Logging
Both provider and consumer take `Peek[F, I]` as an argument, that allows to inject logging logic around the `Request[I] => F[Response[I]]` function. Let's define simple logging peek:
```scala
val peek: Peek[Id, Json] = f => request => {
    println("Request: " + request)
    val response = f(request)
    println("Response: " + response)
    response
}
``` 

#### Failure handling
All meaningful failures that can appear in the library are being transformed into `poppet.Failure`, after what, handled with `poppet.FailureHandler`. Failure handler is a simple polymorphic function from failure to lifted result:
```scala
trait FailureHandler[F[_]] {
    def apply[A](f: Failure): F[A]
}
```
by default, throwing failure handler is being used:
```scala
def throwing[F[_]]: FailureHandler[F] = new FailureHandler[F] {
    override def apply[A](f: Failure): F[A] = throw f
}
```
so if your don't want to deal with JVM exceptions, you can provide your own instance of failure handler. Let's assume you want to pack a failure with `EitherT[Future, String, *]` kind, then failure handler can look like:
```scala
type SR[A] = EitherT[Future, String, A]
val SRFailureHandler = new FailureHandler[SR] {
    override def apply[A](f: Failure): SR[A] = EitherT.leftT(f.getMessage)
}
```
For more info you can check [Http4s with Circe](#examples) example project, it is built around `EitherT[IO, String, *]` kind.

### Manual calls
If your codec has a human-readable format (JSON for example), you can use a provider without consumer (mostly for debug purposes) by generating requests manually. Here is an example of curl call:
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
- run desired example:
    - Http4s with Circe: https://github.com/yakivy/poppet/tree/master/example/http4s
        - run provider: `./mill example.http4s.provider.run`
        - run consumer: `./mill example.http4s.consumer.run`
    - Play Framework with Play Json: https://github.com/yakivy/poppet/tree/master/example/play
        - run provider: `./mill example.play.provider.run`
        - run consumer: `./mill example.play.consumer.run`
        - remove `RUNNING_PID` file manually if services are conflicting with each other
    - And even Spring Framework with Jackson 😲: https://github.com/yakivy/poppet/tree/master/example/spring
        - run provider: `sbt "; project springProviderExample; run"`
        - run consumer: `sbt "; project springConsumerExample; run"`
- put `http://localhost:9002/api/user/1` in the address bar
### Changelog

#### 0.2.0:
- migrate to mill build tool
- add Scala JS and Scala Native support
- add more details to `Can't find processor` exception
- make `FailureHandler` explicit
- rename `poppet.coder` package to `poppet.codec`
- various refactorings and cleanups