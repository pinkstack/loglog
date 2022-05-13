import sbt._

object Dependencies {
  type Version = String
  type Modules = Seq[ModuleID]

  object Versions {
    // val sttp: Version    = "3.5.2"
    val circe: Version     = "0.15.0-M1"
    val logback: Version   = "1.3.0-alpha15"
    val monocle: Version   = "3.1.0"
    val zio: Version       = "2.0.0-RC6"
    val asyncHttp: Version = "2.12.3"
    val tsconfig: Version    = "1.4.2"
  }

  /*
  lazy val sttp: Modules = Seq(
    "com.softwaremill.sttp.client3" %% "core",
    "com.softwaremill.sttp.client3" %% "circe",
    // "com.softwaremill.sttp.client3" %% "httpclient-backend-zio",
    // "com.softwaremill.sttp.client3" %% "async-http-client-backend-zio",
    "com.softwaremill.sttp.client3" %% "httpclient-backend",
    "com.softwaremill.sttp.client3" %% "async-http-client-backend",
    "com.softwaremill.sttp.client3" %% "slf4j-backend"
  ).map(_ % Versions.sttp)
   */

  lazy val asyncHttpClient: Modules = Seq(
    "org.asynchttpclient" % "async-http-client-bom",
    "org.asynchttpclient" % "async-http-client"
  ).map(_ % Versions.asyncHttp)

  lazy val lettuce: Modules = Seq(
    "io.lettuce" % "lettuce-core" % "6.1.8.RELEASE"
  )

  lazy val circe: Modules = Seq(
    "io.circe" %% "circe-core",
    "io.circe" %% "circe-generic",
    "io.circe" %% "circe-parser"
    // "io.circe" %% "circe-optics", // Not yet compatible with Scala 3 (Read: https://github.com/circe/circe-optics/issues/230)
    // "io.circe" %% "circe-fs2"
  ).map(_ % Versions.circe)

  lazy val monocle: Modules = Seq(
    "dev.optics" %% "monocle-core",
    "dev.optics" %% "monocle-macro"
  ).map(_ % Versions.monocle)

  lazy val logging: Modules = Seq(
    "ch.qos.logback" % "logback-classic" % Versions.logback
  )

  lazy val zio: Seq[ModuleID] = Seq(
    "dev.zio" %% "zio",
    "dev.zio" %% "zio-streams"
  ).map(_      % Versions.zio) ++ Seq(
    "dev.zio" %% "zio-json"      % "0.3.0-RC8",
    "dev.zio" %% "zio-json-yaml" % "0.3.0-RC8"
  )

  lazy val zioTest: Seq[ModuleID] = Seq(
    "dev.zio" %% "zio-test",
    "dev.zio" %% "zio-test-sbt",
    "dev.zio" %% "zio-test-magnolia"
  ).map(_ % Versions.zio % "test")

  lazy val influxdb: Seq[ModuleID] = Seq(
    "com.influxdb" % "influxdb-client-java" % "6.0.0"
  )

  lazy val tsconfig: Seq[ModuleID] = Seq(
    "com.typesafe" % "config" % "1.4.2"
  ).map(_ % Versions.tsconfig)

  lazy val resolvers: Seq[MavenRepository] = Seq(
    Resolver.sonatypeRepo("releases"),
    Resolver.sonatypeRepo("snapshots"),
    Resolver.bintrayRepo("websudos", "oss-releases"),
    "Typesafe repository snapshots" at "https://repo.typesafe.com/typesafe/snapshots/",
    "Typesafe repository releases" at "https://repo.typesafe.com/typesafe/releases/",
    "Typesafe repository ivy-releases" at "https://repo.typesafe.com/typesafe/ivy-releases/",
    "Sonatype repo" at "https://oss.sonatype.org/content/groups/scala-tools/",
    "Sonatype releases" at "https://oss.sonatype.org/content/repositories/releases",
    "Sonatype snapshots" at "https://oss.sonatype.org/content/repositories/snapshots",
    "Sonatype staging" at "https://oss.sonatype.org/content/repositories/staging",
    "Java.net Maven2 Repository" at "https://download.java.net/maven/2/",
    "Twitter Repository" at "https://maven.twttr.com"
  )
}
