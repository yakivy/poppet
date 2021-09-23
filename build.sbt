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
    addCompilerPlugin("org.typelevel" %% "kind-projector" % "0.11.0" cross CrossVersion.full)
)

lazy val commonDependencies = Seq(
    libraryDependencies ++= Seq(
        "org.typelevel" %% "cats-core" % versions.cats % "provided,test",
        "org.scalatest" %% "scalatest" % versions.scalatest % "test",
        "org.scala-lang" % "scala-reflect" % scalaVersion.value % "provided"
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
        core,
        circeCoder,
        jacksonCoder,
        playJsonCoder,
    )

lazy val core = project.in(file("core"))
    .settings(name := "poppet-core")
    .settings(commonSettings: _*)
    .settings(publishingSettings: _*)
    .settings(commonDependencies: _*)

lazy val circeCoder = project.in(file("circe"))
    .settings(name := "poppet-circe")
    .settings(commonSettings: _*)
    .settings(publishingSettings: _*)
    .settings(commonDependencies: _*)
    .settings(libraryDependencies ++= Seq(
        "io.circe" %% "circe-core" % versions.circe,
        "io.circe" %% "circe-generic" % versions.circe % "test"
    ))
    .dependsOn(core % "compile->compile;test->test")

lazy val jacksonCoder = project.in(file("jackson"))
    .settings(name := "poppet-jackson")
    .settings(commonSettings: _*)
    .settings(publishingSettings: _*)
    .settings(commonDependencies: _*)
    .settings(libraryDependencies ++= Seq(
        "com.fasterxml.jackson.core" % "jackson-databind" % versions.jackson,
        "com.fasterxml.jackson.module" %% "jackson-module-scala" % versions.jackson,
    ))
    .dependsOn(core % "compile->compile;test->test")

lazy val playJsonCoder = project.in(file("play-json"))
    .settings(name := "poppet-play-json")
    .settings(commonSettings: _*)
    .settings(publishingSettings: _*)
    .settings(commonDependencies: _*)
    .settings(libraryDependencies ++= Seq(
        "com.typesafe.play" %% "play-json" % versions.play,
    ))
    .dependsOn(core % "compile->compile;test->test")

lazy val http4sApiExample = project.in(file("example/http4s/api"))
    .settings(name := "poppet-api-http4s-example")
    .settings(commonSettings: _*)
    .settings(publishingSettings: _*)
    .settings(publish / skip := true)
    .settings(libraryDependencies ++= Seq(
        "org.typelevel" %% "cats-effect" % versions.cats,
        "io.circe" %% "circe-generic" % versions.circe,
    ))
    .dependsOn(circeCoder)

lazy val http4sProviderExample = project.in(file("example/http4s/provider"))
    .settings(name := "poppet-provider-http4s-example")
    .settings(commonSettings: _*)
    .settings(publishingSettings: _*)
    .settings(publish / skip := true)
    .settings(libraryDependencies ++= Seq(
        "org.http4s" %% "http4s-circe" % versions.http4s,
        "org.http4s" %% "http4s-dsl" % versions.http4s,
        "org.http4s" %% "http4s-blaze-server" % versions.http4s,
        "ch.qos.logback" % "logback-classic" % versions.logback,
    ))
    .dependsOn(circeCoder, http4sApiExample)

lazy val http4sConsumerExample = project.in(file("example/http4s/consumer"))
    .settings(name := "poppet-consumer-http4s-example")
    .settings(commonSettings: _*)
    .settings(publishingSettings: _*)
    .settings(publish / skip := true)
    .settings(libraryDependencies ++= Seq(
        "org.http4s" %% "http4s-circe" % versions.http4s,
        "org.http4s" %% "http4s-dsl" % versions.http4s,
        "org.http4s" %% "http4s-blaze-server" % versions.http4s,
        "org.http4s" %% "http4s-blaze-client" % versions.http4s,
        "ch.qos.logback" % "logback-classic" % versions.logback,
    ))
    .dependsOn(circeCoder, http4sApiExample)

lazy val playApiExample = project.in(file("example/play/api"))
    .settings(name := "poppet-api-play-example")
    .settings(commonSettings: _*)
    .settings(publishingSettings: _*)
    .settings(publish / skip := true)
    .dependsOn(playJsonCoder)

lazy val playProviderExample = project.in(file("example/play/provider"))
    .enablePlugins(PlayScala)
    .settings(name := "poppet-provider-play-example")
    .settings(commonSettings: _*)
    .settings(publishingSettings: _*)
    .settings(publish / skip := true)
    .settings(libraryDependencies ++= Seq(
        guice
    ))
    .dependsOn(playJsonCoder, playApiExample)

lazy val playConsumerExample = project.in(file("example/play/consumer"))
    .enablePlugins(PlayScala)
    .settings(name := "poppet-consumer-play-example")
    .settings(commonSettings: _*)
    .settings(publishingSettings: _*)
    .settings(publish / skip := true)
    .settings(libraryDependencies ++= Seq(
        guice, ws
    ))
    .dependsOn(playJsonCoder, playApiExample)

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
        "org.springframework.boot" % "spring-boot-starter-web" % versions.springBoot
    ))
    .settings(mainClass in Compile := Some("poppet.example.spring.Application"))
    .dependsOn(jacksonCoder, springApiExample)

lazy val springConsumerExample = project.in(file("example/spring/consumer"))
    .settings(name := "poppet-consumer-spring-example")
    .settings(commonSettings: _*)
    .settings(publishingSettings: _*)
    .settings(publish / skip := true)
    .settings(libraryDependencies ++= Seq(
        "org.springframework.boot" % "spring-boot-starter-web" % versions.springBoot
    ))
    .settings(mainClass in Compile := Some("poppet.example.spring.Application"))
    .dependsOn(jacksonCoder, springApiExample)