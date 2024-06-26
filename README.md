## Poppet
[![Maven Central](https://img.shields.io/maven-central/v/com.github.yakivy/poppet-core_2.13.svg)](https://mvnrepository.com/search?q=poppet)
[![Sonatype Nexus (Snapshots)](https://img.shields.io/nexus/s/https/oss.sonatype.org/com.github.yakivy/poppet-core_2.13.svg)](https://oss.sonatype.org/content/repositories/snapshots/com/github/yakivy/poppet-core_2.13/)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
<a href="https://typelevel.org/cats/"><img src="https://typelevel.org/cats/img/cats-badge.svg" height="40px" align="right" alt="Cats friendly" /></a>

Poppet is a minimal, type-safe RPC Scala library.

Essential differences from [autowire](https://github.com/lihaoyi/autowire):
- has no explicit macro application `.call`, result of a consumer is an instance of original trait
- has no restricted HKT `Future`, you can specify any monad (has `cats.Monad` typeclass) as HKT for the provider/consumer
- has no forced codec dependencies `uPickle`, you can choose from the list of predefined codecs or easily implement your own codec
- has robust failure handling mechanism
- supports Scala 3 (however method/class generation with macros is still an experimental feature)

### Table of contents
1. [Quick start](#quick-start)
1. [Customizations](#customizations)
    1. [Failure handling](#failure-handling)
1. [Manual calls](#manual-calls)
1. [Limitations](#limitations)
1. [API versioning](#api-versioning)
1. [Examples](#examples)
1. [Changelog](#changelog)

### Quick start
Put cats and poppet dependencies in the build file, let's assume you are using SBT:
```scala
val version = new {
    val cats = "2.10.0"
    val circe = "0.14.6"
    val poppet = "0.4.0"
}

libraryDependencies ++= Seq(
    "org.typelevel" %% "cats-core" % version.cats,

    //to use circe
    "io.circe" %% "circe-core" % version.circe,
    "com.github.yakivy" %% "poppet-circe" % version.poppet,

    //"com.github.yakivy" %% "poppet-upickle" % version.poppet, //to use upickle
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
- `F[_]` - is your service HKT, can be any monad (has `cats.Monad` typeclass);
- `I` - is an intermediate data type that your coding framework works with, can be any serialization format, but it would be easier to choose from existed codec modules as they come with a bunch of predefined codecs;
- `poppet.consumer.Transport` - used to transfer the data between consumer and provider apps, technically it is just a function from `I` to `F[I]`, so you can use anything as long as it can receive/pass the chosen data type;
- `poppet.Codec` - used to convert `I` to domain models and vice versa. Poppet comes with a bunch of modules, where you will hopefully find a favourite codec. If it is not there, you can always try to write your own by providing 2 basic implicits like [here](https://github.com/yakivy/poppet/blob/master/circe/src/poppet/codec/circe/instances/CirceCodecInstances.scala);
- `poppet.CodecK` - used to convert method return HKT to `F` and vice versa. It's needed only if return HKT differs from your service HKT, compilation errors will hint you what codecs are absent;
- `poppet.FailureHandler[F[_]]` - used to handle internal failures, more info you can find [here](#failure-handling);

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
so if your don't want to deal with JVM exceptions, you can provide your own instance of failure handler. Let's assume you want to pack a failure with `EitherT[Future, String, *]` HKT, then failure handler can look like:
```scala
type SR[A] = EitherT[Future, String, A]
val SRFailureHandler = new FailureHandler[SR] {
    override def apply[A](f: Failure): SR[A] = EitherT.leftT(f.getMessage)
}
```
For more info you can check [Http4s with Circe](#examples) example project, it is built around `EitherT[IO, String, *]` HKT.

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

### Limitations
You can generate consumer/provider almost from any Scala trait (or Java interface 😲). It can have non-abstract members, methods with default arguments, methods with multiple argument lists, varargs, etc... But there are several limitations:
- you cannot overload methods with the same argument names, because for the sake of simplicity argument names are being used as a part of the request, for more info check [manual calls](#manual-calls) section:
```scala
//compiles
def apply(a: String): Boolean = ???
def apply(b: Int): Boolean = ???

// doesn't compile
def apply(a: String): Boolean = ???
def apply(a: Int): Boolean = ???
```
- trait/method type parameters should be fully qualified, because codecs are resolved at consumer/provider generation rather than at the method call:
```scala
//compiles
trait A[T] {
    def apply(t: T): Boolean
}

//doesn't compile
trait A {
    def apply[T](t: T): Boolean
}
trait A {
    type T
    def apply(t: T): Boolean
}
```
- trait should not have arguments

### API versioning
The goal of the library is to closely resemble typical Scala traits, so same binary compatibility approaches can also be applied for API versioning, for example:
- when you want to change method signature, add new method and deprecate old one, (important note: argument name is a part of signature in poppet, for more info check [limitations](#limitations) section):
```scala
@deprecared def apply(a: String): Boolean = ???
def apply(b: Int): Boolean = ???
```
- if you are tolerant to binary incompatible changes, you can modify argument/return types without creating new method, but ensure that codecs are compatible:
```scala
def apply(a: String): Boolean = ???
//if Email is serialized as a String, method can be updated to
def apply(a: Email): Boolean = ???
```
- when you want to remove method, deprecate it and remove after all consumers are updated to the new version
- when you want to change service name, provide new service (you can extend it from the old one) and deprecate old one:
```scala
@deprecated trait A
trait B extends A

Provider[..., ...]()
    .service[A](bImpl)
    .service[B](bImpl)
```

### Examples
- run desired example:
    - Http4s with Circe: https://github.com/yakivy/poppet/tree/master/example/http4s-circe
        - run provider: `./mill example.http4s-circe.provider.run`
        - run consumer: `./mill example.http4s-circe.consumer.run`
    - Play Framework with Play Json: https://github.com/yakivy/poppet/tree/master/example/play
        - run provider: `./mill example.play.provider.run`
        - run consumer: `./mill example.play.consumer.run`
        - remove `RUNNING_PID` file manually if services are conflicting with each other
    - And even Spring Framework with Jackson 😲: https://github.com/yakivy/poppet/tree/master/example/spring-jackson
        - run provider: `./mill example.spring-jackson.provider.run`
        - run consumer: `./mill example.spring-jackson.consumer.run`
    - Tapir with Sttp with FS2 with Circe (supports streaming): https://github.com/yakivy/poppet/tree/master/example/tapir-sttp-fs2-circe
      - run provider: `./mill example.tapir-sttp-fs2-circe.provider.run`
      - run consumer: `./mill example.tapir-sttp-fs2-circe.consumer.run`
- put `http://localhost:9002/api/user/1` in the address bar
- put `http://localhost:9002/api/user` in the address bar if transport supports streaming

### Roadmap
- add action (including argument name) to codec
- throw an exception on duplicated service processor
- separate `.service[S]` and `.service[G[_], S]` to simplify codec resolution
- check that passed class is a trait and doesn't have arguments to prevent obscure error from compiler
- check that all abstract methods are public

### Changelog
#### 0.4.x:
- simplify transport and provider response
- remove peek
- remove ObjectMapper creation from Jackson codec, ask for it implicitly

#### 0.3.x:
- fix compilation errors for methods with varargs
- fix codec resolution for id (`I => I`) codecs
- add Scala 3 support

#### 0.2.x:
- fix compilation error message for ambiguous implicits
- fix processor compilation for complex types
- migrate to mill build tool
- add Scala JS and Scala Native support
- add more details to `Can't find processor` exception
- make `FailureHandler` explicit
- rename `poppet.coder` package to `poppet.codec`
- various refactorings and cleanups