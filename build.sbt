import Dependencies._
import sbt.Keys.resolvers

import scala.sys.process._

ThisBuild / version      := "0.0.1"
ThisBuild / scalaVersion := "3.1.1"
ThisBuild / scalacOptions := Seq(
  // "-Ykind-projector:underscores",
  "-source:future",
  "-language:adhocExtensions"
)

ThisBuild / publishArtifact := false
ThisBuild / skip / publish  := true

lazy val root = (project in file("."))
  .enablePlugins(JavaServerAppPackaging, DockerPlugin)
  .settings(
    name := "loglog",
    libraryDependencies ++= {
      zio ++ zioTest ++ asyncHttpClient ++ circe ++ influxdb ++ tsconfig ++ logging
    },
    resolvers           := Dependencies.resolvers,
    Compile / mainClass := Some("com.pinkstack.loglog.CollectorApp"),
    fork                := true,
    testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework")
  )
  .settings(DockerSettings.settings: _*)

lazy val deploy = taskKey[Unit]("Execute the shell script")
deploy := ("""./bin/deploy.sh""" !)

lazy val reStartContainer = taskKey[Unit]("Restart container")
reStartContainer := ("./bin/loglog-dev.sh up -d --no-deps --no-build --force-recreate loglog".!)
