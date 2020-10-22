lazy val versions = new {
    val scala213 = "2.13.3"
    val scala212 = "2.12.10"
    val cats = "2.0.0"
    val scalatest = "3.0.8"
    val circe = "0.13.0"
    val play = "2.8.1"
    val jackson = "2.11.1"
    val http4s = "0.21.8"
    val logback = "1.2.3"
    val springBoot = "2.3.1.RELEASE"
}

lazy val commonSettings = Seq(
    organization := "com.github.yakivy",
    scmInfo := Some(ScmInfo(
        url("https://github.com/yakivy/poppet"),
        "scm:git@github.com:yakivy/poppet.git"
    )),
    developers := List(Developer(
        id = "yakivy",
        name = "Yakiv Yereskovskyi",
        email = "yakiv.yereskovskyi@gmail.com",
        url = url("https://github.com/yakivy")
    )),
    licenses := Seq("MIT License" -> url("http://www.opensource.org/licenses/mit-license.html")),
    homepage := Some(url("https://github.com/yakivy/poppet")),
    scalaVersion := versions.scala213,
    crossScalaVersions := Seq(versions.scala213, versions.scala212),
)

lazy val commonDependencies = Seq(
    libraryDependencies ++= Seq(
        "org.typelevel" %% "cats-core" % versions.cats % "test,provided",
        "org.scalatest" %% "scalatest" % versions.scalatest % "test",
        "org.scala-lang" % "scala-reflect" % scalaVersion.value
    )
)

lazy val publishingSettings = Seq(
    publishTo := {
        val nexus = "https://oss.sonatype.org/"
        if (isSnapshot.value) Some("snapshots" at nexus + "content/repositories/snapshots")
        else Some("releases" at nexus + "service/local/staging/deploy/maven2")
    },
    publishMavenStyle := true,
    pomIncludeRepository := { _ => false },
)

lazy val root = project.in(file("."))
    .settings(name := "poppet")
    .settings(publishingSettings: _*)
    .settings(publish / skip := true)
    .aggregate(
        coder,
        circeCoder,
        jacksonCoder,
        playJsonCoder,
        provider,
        consumer,
    )

lazy val coder = project.in(file("coder/core"))
    .settings(name := "poppet-coder-core")
    .settings(commonSettings: _*)
    .settings(publishingSettings: _*)
    .settings(commonDependencies: _*)

lazy val circeCoder = project.in(file("coder/circe"))
    .settings(name := "poppet-coder-circe")
    .settings(commonSettings: _*)
    .settings(publishingSettings: _*)
    .settings(commonDependencies: _*)
    .settings(libraryDependencies ++= Seq(
        "io.circe" %% "circe-parser" % versions.circe % "test,provided",
        "io.circe" %% "circe-generic" % versions.circe % "test,provided"
    ))
    .dependsOn(coder % "compile->compile;test->test")

lazy val jacksonCoder = project.in(file("coder/jackson"))
    .settings(name := "poppet-coder-jackson")
    .settings(commonSettings: _*)
    .settings(publishingSettings: _*)
    .settings(commonDependencies: _*)
    .settings(libraryDependencies ++= Seq(
        "com.fasterxml.jackson.core" % "jackson-databind" % versions.jackson % "test,provided",
        "com.fasterxml.jackson.module" %% "jackson-module-scala" % versions.jackson % "test,provided",
    ))
    .dependsOn(coder % "compile->compile;test->test")

lazy val playJsonCoder = project.in(file("coder/play-json"))
    .settings(name := "poppet-coder-play-json")
    .settings(commonSettings: _*)
    .settings(publishingSettings: _*)
    .settings(commonDependencies: _*)
    .settings(libraryDependencies ++= Seq(
        "com.typesafe.play" %% "play-json" % versions.play % "test,provided",
    ))
    .dependsOn(coder % "compile->compile;test->test")

lazy val provider = project.in(file("provider"))
    .settings(name := "poppet-provider")
    .settings(commonSettings: _*)
    .settings(publishingSettings: _*)
    .settings(commonDependencies: _*)
    .dependsOn(coder % "compile->compile;test->test")

lazy val consumer = project.in(file("consumer"))
    .settings(name := "poppet-consumer")
    .settings(commonSettings: _*)
    .settings(publishingSettings: _*)
    .settings(commonDependencies: _*)
    .dependsOn(coder % "compile->compile;test->test")

lazy val http4sApiExample = project.in(file("example/http4s/api"))
    .settings(name := "poppet-api-http4s-example")
    .settings(commonSettings: _*)
    .settings(publishingSettings: _*)
    .settings(publish / skip := true)
    .settings(libraryDependencies ++= Seq(
        "org.typelevel" %% "cats-effect" % versions.cats,
    ))

lazy val http4sProviderExample = project.in(file("example/http4s/provider"))
    .settings(name := "poppet-provider-http4s-example")
    .settings(commonSettings: _*)
    .settings(publishingSettings: _*)
    .settings(publish / skip := true)
    .settings(libraryDependencies ++= Seq(
        "io.circe" %% "circe-generic" % versions.circe,
        "org.http4s" %% "http4s-circe" % versions.http4s,
        "org.http4s" %% "http4s-dsl" % versions.http4s,
        "org.http4s" %% "http4s-blaze-server" % versions.http4s,
        "ch.qos.logback" % "logback-classic" % versions.logback,
    ))
    .dependsOn(circeCoder, provider, http4sApiExample)

lazy val http4sConsumerExample = project.in(file("example/http4s/consumer"))
    .settings(name := "poppet-consumer-http4s-example")
    .settings(commonSettings: _*)
    .settings(publishingSettings: _*)
    .settings(publish / skip := true)
    .settings(libraryDependencies ++= Seq(
        "io.circe" %% "circe-generic" % versions.circe,
        "org.http4s" %% "http4s-circe" % versions.http4s,
        "org.http4s" %% "http4s-dsl" % versions.http4s,
        "org.http4s" %% "http4s-blaze-server" % versions.http4s,
        "org.http4s" %% "http4s-blaze-client" % versions.http4s,
        "ch.qos.logback" % "logback-classic" % versions.logback,
    ))
    .dependsOn(circeCoder, consumer, http4sApiExample)

lazy val playApiExample = project.in(file("example/play/api"))
    .settings(name := "poppet-api-play-example")
    .settings(commonSettings: _*)
    .settings(publishingSettings: _*)
    .settings(libraryDependencies ++= Seq(
        "com.typesafe.play" %% "play-json" % versions.play,
    ))
    .settings(publish / skip := true)

lazy val playProviderExample = project.in(file("example/play/provider"))
    .enablePlugins(PlayScala)
    .settings(name := "poppet-provider-play-example")
    .settings(commonSettings: _*)
    .settings(publishingSettings: _*)
    .settings(publish / skip := true)
    .settings(libraryDependencies ++= Seq(
        "org.typelevel" %% "cats-core" % versions.cats,
        guice
    ))
    .dependsOn(playJsonCoder, provider, playApiExample)

lazy val playConsumerExample = project.in(file("example/play/consumer"))
    .enablePlugins(PlayScala)
    .settings(name := "poppet-consumer-play-example")
    .settings(commonSettings: _*)
    .settings(publishingSettings: _*)
    .settings(publish / skip := true)
    .settings(libraryDependencies ++= Seq(
        "org.typelevel" %% "cats-core" % versions.cats,
        guice, ws
    ))
    .dependsOn(playJsonCoder, consumer, playApiExample)

lazy val springApiExample = project.in(file("example/spring/api"))
    .settings(name := "poppet-api-spring-example")
    .settings(commonSettings: _*)
    .settings(publishingSettings: _*)
    .settings(publish / skip := true)

lazy val springProviderExample = project.in(file("example/spring/provider"))
    .settings(name := "poppet-provider-spring-example")
    .settings(commonSettings: _*)
    .settings(publishingSettings: _*)
    .settings(publish / skip := true)
    .settings(libraryDependencies ++= Seq(
        "org.typelevel" %% "cats-core" % versions.cats,
        "com.fasterxml.jackson.module" %% "jackson-module-scala" % versions.jackson,
        "org.springframework.boot" % "spring-boot-starter-web" % versions.springBoot
    ))
    .settings(mainClass in Compile := Some("poppet.example.spring.Application"))
    .dependsOn(jacksonCoder, provider, springApiExample)

lazy val springConsumerExample = project.in(file("example/spring/consumer"))
    .settings(name := "poppet-consumer-spring-example")
    .settings(commonSettings: _*)
    .settings(publishingSettings: _*)
    .settings(publish / skip := true)
    .settings(libraryDependencies ++= Seq(
        "org.typelevel" %% "cats-core" % versions.cats,
        "com.fasterxml.jackson.module" %% "jackson-module-scala" % versions.jackson,
        "org.springframework.boot" % "spring-boot-starter-web" % versions.springBoot
    ))
    .settings(mainClass in Compile := Some("poppet.example.spring.Application"))
    .dependsOn(jacksonCoder, consumer, springApiExample)