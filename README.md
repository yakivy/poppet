## Poppet
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
1. [Error handling](#error-handling)
1. [Examples](#examples)
1. [Notes](#notes)

### Quick start
Put library version in the build file:
```scala
val poppetVersion = "0.0.1.4-SNAPSHOT"
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
Add spring poppet coder and provider dependencies to the build file, let's assume you are using sbt:
```scala
libraryDependencies += Seq(
    "com.github.yakivy" %% "poppet-coder-jackson" % poppetVersion,
    "com.github.yakivy" %% "poppet-provider-spring" % poppetVersion
)
```
Create separate scala provider generator, keep in mind that only abstract methods of the service type will be exposed:
```scala
import poppet.coder.jackson.all._
import poppet.provider.spring.all._

object ProviderGenerator {
    def apply(
        userService: UserService
    ): RequestEntity[Array[Byte]] => ResponseEntity[Array[Byte]] = {
        Provider(
            SpringServer(),
            JacksonCoder())(
            ProviderProcessor(userService).generate()
        ).materialize()
    }
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
Add spring poppet coder and consumer dependencies to the build file:
```scala
libraryDependencies += Seq(
    "com.github.yakivy" %% "poppet-coder-jackson" % poppetVersion,
    "com.github.yakivy" %% "poppet-consumer-spring" % poppetVersion
)
```
Create separate scala consumer generator (can be materialized once and shared everywhere):
```scala
import poppet.coder.jackson.all._
import poppet.consumer.spring.all._

object ConsumerGenerator {
    def userService(restTemplate: RestTemplate): UserService = Consumer(
        SpringClient(s"http://${providerHostName}:9001/api/service")(restTemplate),
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