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

lazy val root = (project in file("."))
  .enablePlugins(JavaServerAppPackaging, DockerPlugin)
  .settings(
    name := "loglog",
    libraryDependencies ++= {
      zio ++ asyncHttpClient ++ circe ++ influxdb ++ lettuce ++ logging
    },
    resolvers           := Dependencies.resolvers,
    Compile / mainClass := Some("com.pinkstack.loglog.CollectorApp"),
    fork                := true
  )
  .settings(DockerSettings.settings: _*)

lazy val deploy = taskKey[Unit]("Execute the shell script")
deploy := ("""./bin/deploy.sh""" !)

lazy val reStartContainer = taskKey[Unit]("Restart container")
reStartContainer := ("./bin/loglog-dev.sh up -d --no-deps --no-build --force-recreate loglog".!)
