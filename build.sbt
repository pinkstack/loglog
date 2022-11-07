import Dependencies._
import sbt.Keys.resolvers

import scala.sys.process._

ThisBuild / scalaVersion  := "3.2.0"
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

// Custom tasks
lazy val deploy = taskKey[Unit]("Execute the shell script")
deploy := ("""./bin/deploy.sh""" !)

lazy val reStartContainer = taskKey[Unit]("Restart container")
reStartContainer := ("./bin/loglog-dev.sh up -d --no-deps --no-build --force-recreate loglog".!)

// Publishing
ThisBuild / publishArtifact := false
ThisBuild / publishTo       := Some(Resolver.file("Unused transient repository", file("target/loglog-root")))
ThisBuild / skip / publish  := true
