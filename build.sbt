import Dependencies._
import sbt.Keys.resolvers

import scala.sys.process._

ThisBuild / version      := "0.0.2"
ThisBuild / scalaVersion := "3.1.3"
ThisBuild / scalacOptions := Seq(
  // "-Ykind-projector:underscores",
  "-source:future",
  "-language:adhocExtensions",
  "-language:implicitConversions"
)

lazy val backend = (project in file("backend"))
  .enablePlugins(JavaServerAppPackaging, DockerPlugin)
  .settings(
    libraryDependencies ++= {
      zio ++ zioTest ++ asyncHttpClient ++ circe ++ influxdb ++ tsconfig ++ logging
    },
    resolvers           := Dependencies.resolvers,
    Compile / mainClass := Some("com.pinkstack.loglog.CollectorApp"),
    fork                := true,
    testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework")
  )
  .settings(DockerSettings.settings: _*)

lazy val frontend = (project in file("frontend"))
  .enablePlugins(ScalaJSPlugin)
  .settings(
    scalaJSUseMainModuleInitializer := true,
    scalaJSLinkerConfig ~= { _.withModuleKind(ModuleKind.ESModule) },
    libraryDependencies ++= Seq(
      "dev.zio"           %%% "zio"                  % Versions.zio,
      "dev.zio"           %%% "zio-test"             % Versions.zio % "test",
      "dev.zio"           %%% "zio-test-sbt"         % Versions.zio % "test",
      "com.lihaoyi"       %%% "scalatags"            % "0.11.1",
      "io.github.cquiroz" %%% "scala-java-time"      % "2.4.0",
      "io.github.cquiroz" %%% "scala-java-time-tzdb" % "2.4.0",
      "org.scala-js"      %%% "scalajs-dom"          % "2.2.0"
    )
  )

// Custom tasks
lazy val deploy = taskKey[Unit]("Execute the shell script")
deploy := ("""./bin/deploy.sh""" !)

lazy val reStartContainer = taskKey[Unit]("Restart container")
reStartContainer := ("./bin/loglog-dev.sh up -d --no-deps --no-build --force-recreate loglog".!)

// Publishing
ThisBuild / publishArtifact := false
ThisBuild / publishTo       := Some(Resolver.file("Unused transient repository", file("target/loglog-root")))
ThisBuild / skip / publish  := true
