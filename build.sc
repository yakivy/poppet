import $ivy.`com.lihaoyi::mill-contrib-playlib:$MILL_VERSION`

import mill._
import mill.scalalib._
import mill.scalajslib._
import mill.scalanativelib._
import mill.scalalib.publish._
import mill.playlib._

object versions {
    val publish = "0.3.5"

    val scala212 = "2.12.18"
    val scala213 = "2.13.12"
    val scala3 = "3.3.0"
    val scalaJs = "1.13.2"
    val scalaNative = "0.4.16"
    val scalatest = "3.2.14"
    val cats = "2.10.0"

    val upickle = "2.0.0"
    val circe = "0.14.6"
    val playJson = "2.9.4"
    val jackson = "2.13.5"

    val catsEffect = "3.4.1"
    val http4s = "0.23.12"
    val play = "2.8.18"
    val logback = "1.2.11"
    val springBoot = "2.7.5"

    val cross2 = Seq(scala212, scala213)
    val cross3 = Seq(scala3)
    val cross = cross2 ++ cross3
}

trait CommonPublishModule extends PublishModule with CrossScalaModule {
    override def publishVersion = versions.publish
    override def pomSettings = PomSettings(
        description = artifactName(),
        organization = "com.github.yakivy",
        url = "https://github.com/yakivy/poppet",
        licenses = Seq(License.MIT),
        versionControl = VersionControl.github("yakivy", "poppet"),
        developers = Seq(Developer("yakivy", "Yakiv Yereskovskyi", "https://github.com/yakivy"))
    )
    override def compileIvyDeps = super.compileIvyDeps() ++ Agg(
        ivy"org.typelevel::cats-core:${versions.cats}",
    ) ++ (
        if (crossScalaVersion == versions.scala3) Agg.empty[Dep]
        else Agg(ivy"org.scala-lang:scala-reflect:${scalaVersion()}")
    )
    override def millSourcePath = super.millSourcePath / os.up
    override def scalacOptions = super.scalacOptions() ++ (
        if (crossScalaVersion == versions.scala3) Seq("-Xcheck-macros", "-explain")
        else Seq.empty[String]
    )
}

trait CommonPublishTestModule extends ScalaModule with TestModule {
    override def ivyDeps = super.ivyDeps() ++ Agg(
        ivy"org.scalatest::scalatest::${versions.scalatest}",
        ivy"org.typelevel::cats-core::${versions.cats}",
    )
    override def testFramework = "org.scalatest.tools.Framework"
}

trait CommonPublishJvmModule extends CommonPublishModule {
    trait CommonPublishCrossModuleTests extends CommonPublishTestModule with ScalaTests
}

trait CommonPublishJsModule extends CommonPublishModule with ScalaJSModule {
    def scalaJSVersion = versions.scalaJs
    trait CommonPublishCrossModuleTests extends CommonPublishTestModule with ScalaTests
}

trait CommonPublishNativeModule extends CommonPublishModule with ScalaNativeModule {
    def scalaNativeVersion = versions.scalaNative
    trait CommonPublishCrossModuleTests extends CommonPublishTestModule with ScalaTests
}

object core extends Module {
    trait CommonModule extends CommonPublishModule {
        override def artifactName = "poppet-core"

        trait CommonModuleTests extends ScalaTests {
            override def ivyDeps = super.ivyDeps() ++ Agg(
                ivy"com.lihaoyi::upickle::${versions.upickle}",
            )
        }
    }

    object jvm extends Cross[JvmModule](versions.cross)
    trait JvmModule extends CommonModule with CommonPublishJvmModule {
        object test extends CommonModuleTests with CommonPublishCrossModuleTests {
            override def moduleDeps = super.moduleDeps ++ Seq(upickle.jvm())
        }
    }

    object js extends Cross[JsModule](versions.cross)
    trait JsModule extends CommonModule with CommonPublishJsModule {
        object test extends CommonModuleTests with CommonPublishCrossModuleTests {
            override def moduleDeps = super.moduleDeps ++ Seq(upickle.js())
        }
    }

    object native extends Cross[NativeModule](versions.cross)
    trait NativeModule extends CommonModule with CommonPublishNativeModule {
        object test extends CommonModuleTests with CommonPublishCrossModuleTests {
            override def moduleDeps = super.moduleDeps ++ Seq(upickle.native())
        }
    }
}

object upickle extends Module {
    trait CommonModule extends CommonPublishModule {
        override def artifactName = "poppet-upickle"

        override def compileIvyDeps = super.compileIvyDeps() ++ Agg(
            ivy"com.lihaoyi::upickle::${versions.upickle}",
        )

        trait CommonModuleTests extends ScalaTests {
            override def ivyDeps = super.ivyDeps() ++ Agg(
                ivy"com.lihaoyi::upickle::${versions.upickle}",
            )
        }
    }

    object jvm extends Cross[JvmModule](versions.cross)
    trait JvmModule extends CommonModule with CommonPublishJvmModule {
        override def moduleDeps = super.moduleDeps ++ Seq(core.jvm())
        object test extends CommonModuleTests with CommonPublishCrossModuleTests {
            override def moduleDeps = super.moduleDeps ++ Seq(core.jvm().test)
        }
    }

    object js extends Cross[JsModule](versions.cross)
    trait JsModule extends CommonModule with CommonPublishJsModule {
        override def moduleDeps = super.moduleDeps ++ Seq(core.js())
        object test extends CommonModuleTests with CommonPublishCrossModuleTests {
            override def moduleDeps = super.moduleDeps ++ Seq(core.js().test)
        }
    }

    object native extends Cross[NativeModule](versions.cross)
    trait NativeModule extends CommonModule with CommonPublishNativeModule {
        override def moduleDeps = super.moduleDeps ++ Seq(core.native())
        object test extends CommonModuleTests with CommonPublishCrossModuleTests {
            override def moduleDeps = super.moduleDeps ++ Seq(core.native().test)
        }
    }
}

object circe extends Module {
    trait CommonModule extends CommonPublishModule {
        override def artifactName = "poppet-circe"

        override def compileIvyDeps = super.compileIvyDeps() ++ Agg(
            ivy"io.circe::circe-core::${versions.circe}",
        )

        trait CommonModuleTests extends ScalaTests {
            override def ivyDeps = super.ivyDeps() ++ Agg(
                ivy"io.circe::circe-core::${versions.circe}",
                ivy"io.circe::circe-generic::${versions.circe}",
            )
        }
    }

    object jvm extends Cross[JvmModule](versions.cross)
    trait JvmModule extends CommonModule with CommonPublishJvmModule {
        override def moduleDeps = super.moduleDeps ++ Seq(core.jvm())
        object test extends CommonModuleTests with CommonPublishCrossModuleTests {
            override def moduleDeps = super.moduleDeps ++ Seq(core.jvm().test)
        }
    }

    object js extends Cross[JsModule](versions.cross)
    trait JsModule extends CommonModule with CommonPublishJsModule {
        override def moduleDeps = super.moduleDeps ++ Seq(core.js())
        object test extends CommonModuleTests with CommonPublishCrossModuleTests {
            override def moduleDeps = super.moduleDeps ++ Seq(core.js().test)
        }
    }

    object native extends Cross[NativeModule](versions.cross)
    trait NativeModule extends CommonModule with CommonPublishNativeModule {
        override def moduleDeps = super.moduleDeps ++ Seq(core.native())
        object test extends CommonModuleTests with CommonPublishCrossModuleTests {
            override def moduleDeps = super.moduleDeps ++ Seq(core.native().test)
        }
    }
}

object `play-json` extends Module {
    trait CommonModule extends CommonPublishModule {
        override def artifactName = "poppet-play-json"

        override def compileIvyDeps = super.compileIvyDeps() ++ Agg(
            ivy"com.typesafe.play::play-json::${versions.playJson}",
        )

        trait CommonModuleTests extends ScalaTests {
            override def ivyDeps = super.ivyDeps() ++ Agg(
                ivy"com.typesafe.play::play-json::${versions.playJson}",
            )
        }
    }

    object jvm extends Cross[JvmModule](versions.cross2)
    trait JvmModule extends CommonModule with CommonPublishJvmModule {
        override def moduleDeps = super.moduleDeps ++ Seq(core.jvm())
        object test extends CommonModuleTests with CommonPublishCrossModuleTests {
            override def moduleDeps = super.moduleDeps ++ Seq(core.jvm().test)
        }
    }

    object js extends Cross[JsModule](versions.cross2)
    trait JsModule extends CommonModule with CommonPublishJsModule {
        override def moduleDeps = super.moduleDeps ++ Seq(core.js())
        object test extends CommonModuleTests with CommonPublishCrossModuleTests {
            override def moduleDeps = super.moduleDeps ++ Seq(core.js().test)
        }
    }
}

object jackson extends Module {
    trait CommonModule extends CommonPublishModule {
        override def artifactName = "poppet-jackson"

        override def compileIvyDeps = super.compileIvyDeps() ++ Agg(
            ivy"com.fasterxml.jackson.core:jackson-databind::${versions.jackson}",
            ivy"com.fasterxml.jackson.module::jackson-module-scala::${versions.jackson}",
        )

        trait CommonModuleTests extends ScalaTests {
            override def ivyDeps = super.ivyDeps() ++ Agg(
                ivy"com.fasterxml.jackson.core:jackson-databind::${versions.jackson}",
                ivy"com.fasterxml.jackson.module::jackson-module-scala::${versions.jackson}",
            )
        }
    }

    object jvm extends Cross[JvmModule](versions.cross)
    trait JvmModule extends CommonModule with CommonPublishJvmModule {
        override def moduleDeps = super.moduleDeps ++ Seq(core.jvm())
        object test extends CommonModuleTests with CommonPublishCrossModuleTests {
            override def moduleDeps = super.moduleDeps ++ Seq(core.jvm().test)
        }
    }
}

object example extends Module {
    object http4s extends Module {
        trait CommonModule extends ScalaModule {
            override def scalaVersion = versions.scala3
            override def ivyDeps = super.ivyDeps() ++ Agg(
                ivy"org.typelevel::cats-core::${versions.cats}",
                ivy"org.typelevel::cats-effect::${versions.catsEffect}",
                ivy"io.circe::circe-generic::${versions.circe}",
            )
            override def moduleDeps = super.moduleDeps ++ Seq(circe.jvm(versions.scala3))
        }
        object api extends CommonModule
        object consumer extends CommonModule {
            override def ivyDeps = super.ivyDeps() ++ Agg(
                ivy"org.http4s::http4s-circe::${versions.http4s}",
                ivy"org.http4s::http4s-dsl::${versions.http4s}",
                ivy"org.http4s::http4s-blaze-server::${versions.http4s}",
                ivy"org.http4s::http4s-blaze-client::${versions.http4s}",
                ivy"ch.qos.logback:logback-classic:${versions.logback}",
            )
            override def moduleDeps = super.moduleDeps ++ Seq(api)
        }
        object provider extends CommonModule {
            override def ivyDeps = super.ivyDeps() ++ Agg(
                ivy"org.http4s::http4s-circe::${versions.http4s}",
                ivy"org.http4s::http4s-dsl::${versions.http4s}",
                ivy"org.http4s::http4s-blaze-server::${versions.http4s}",
                ivy"ch.qos.logback:logback-classic:${versions.logback}",
            )
            override def moduleDeps = super.moduleDeps ++ Seq(api)
        }
    }

    object play extends Module {
        trait CommonModule extends ScalaModule {
            override def scalaVersion = versions.scala213
            override def ivyDeps = super.ivyDeps() ++ Agg(
                ivy"org.typelevel::cats-core::${versions.cats}",
                ivy"com.typesafe.play::play-json::${versions.playJson}",
            )
            override def moduleDeps = super.moduleDeps ++ Seq(`play-json`.jvm(versions.scala213))
        }
        object api extends CommonModule
        object consumer extends CommonModule with PlayApiModule {
            override def playVersion = versions.play
            override def ivyDeps = super.ivyDeps() ++ Agg(
                ws()
            )
            override def moduleDeps = super.moduleDeps ++ Seq(api)
        }
        object provider extends CommonModule with PlayApiModule {
            override def playVersion = versions.play
            override def moduleDeps = super.moduleDeps ++ Seq(api)
        }
    }

    object spring extends Module {
        trait CommonModule extends ScalaModule {
            override def scalaVersion = versions.scala213
            override def ivyDeps = super.ivyDeps() ++ Agg(
                ivy"org.typelevel::cats-core::${versions.cats}",
                ivy"com.fasterxml.jackson.core:jackson-databind::${versions.jackson}",
                ivy"com.fasterxml.jackson.module::jackson-module-scala::${versions.jackson}",
            )
            override def moduleDeps = super.moduleDeps ++ Seq(jackson.jvm(versions.scala213))
            override def javacOptions = Seq("-source", "1.8", "-target", "1.8")
        }
        object api extends CommonModule
        object consumer extends CommonModule {
            override def finalMainClass = "poppet.example.spring.consumer.Application"
            override def ivyDeps = super.ivyDeps() ++ Agg(
                ivy"org.springframework.boot:spring-boot-starter-web:${versions.springBoot}",
            )
            override def moduleDeps = super.moduleDeps ++ Seq(api)
        }
        object provider extends CommonModule {
            override def finalMainClass = "poppet.example.spring.provider.Application"
            override def ivyDeps = super.ivyDeps() ++ Agg(
                ivy"org.springframework.boot:spring-boot-starter-web:${versions.springBoot}",
            )
            override def moduleDeps = super.moduleDeps ++ Seq(api)
        }
    }
}