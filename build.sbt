lazy val versions = new {
    val scala213 = "2.13.1"
    val scala212 = "2.12.10"
    val cats = "2.0.0"
    val scalatest = "3.0.8"
    val play = "2.8.1"
}

scalacOptions ++= Seq(
    "-Ymacro-debug-lite"
)

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
        playCoder,
        provider,
        playProvider,
        consumer,
        playConsumer
    )

lazy val coder = project.in(file("coder"))
    .settings(name := "poppet-coder")
    .settings(commonSettings: _*)
    .settings(publishingSettings: _*)
    .settings(commonDependencies: _*)

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

lazy val playCoder = project.in(file("coder/play"))
    .settings(name := "poppet-coder-play")
    .settings(commonSettings: _*)
    .settings(publishingSettings: _*)
    .dependsOn(coder % "compile->compile;test->test")
    .settings(Seq(
        libraryDependencies ++= Seq(
            "com.typesafe.play" %% "play-json" % versions.play % "test,provided",
        )
    ))

lazy val playProvider = project.in(file("provider/play"))
    .settings(name := "poppet-provider-play")
    .settings(commonSettings: _*)
    .settings(publishingSettings: _*)
    .dependsOn(provider % "compile->compile;test->test")
    .settings(libraryDependencies ++= Seq(
        "com.typesafe.play" %% "play" % versions.play % "test,provided",
    ))

lazy val playConsumer = project.in(file("consumer/play"))
    .settings(name := "poppet-consumer-play")
    .settings(commonSettings: _*)
    .settings(publishingSettings: _*)
    .dependsOn(consumer % "compile->compile;test->test")

lazy val playApiExample = project.in(file("example/play/api"))
    .settings(name := "poppet-api-play-example")
    .settings(commonSettings: _*)
    .settings(publishingSettings: _*)
    .settings(publish / skip := true)

lazy val playProviderExample = project.in(file("example/play/provider"))
    .enablePlugins(PlayScala)
    .settings(name := "poppet-provider-play-example")
    .settings(commonSettings: _*)
    .settings(publishingSettings: _*)
    .settings(publish / skip := true)
    .settings(libraryDependencies ++= Seq(
        "org.typelevel" %% "cats-core" % versions.cats
    ))
    .settings(libraryDependencies ++= Seq(
        guice
    ))
    .dependsOn(playCoder, playProvider, playApiExample)