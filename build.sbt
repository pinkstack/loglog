import Dependencies._
import sbt.Keys.resolvers

ThisBuild / version      := "0.0.1"
ThisBuild / scalaVersion := "3.1.1"
ThisBuild / scalacOptions := Seq(
  // "-Ykind-projector:underscores",
  "-source:future",
  "-language:adhocExtensions"
)

lazy val root = (project in file("."))
  .settings(
    name := "loglog",
    libraryDependencies ++= {
      zio ++ asyncHttpClient ++ circe ++ influxdb ++ logging
    },
    resolvers           := Dependencies.resolvers,
    Compile / mainClass := Some("com.pinkstack.loglog.CollectorApp"),
    fork                := true
  )
