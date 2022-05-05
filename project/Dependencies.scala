import sbt._

object Dependencies {
  type Version = String
  type Modules = Seq[ModuleID]

  object Versions {
    val cats: Version       = "2.7.0"
    val catsEffect: Version = "3.3.11"
    val fs2: Version        = "3.2.7"
    val decline: Version    = "2.2.0"
    val sttp: Version       = "3.5.2"
    val circe: Version      = "0.14.0" // "0.15.0-M1" (fs2 works only on 0.14.0)
    val logback: Version    = "1.3.0-alpha14"
    val log4cats: Version   = "2.2.0"
    val doobie: Version     = "1.0.0-RC2"
    val monocle: Version    = "3.1.0"
    val zio: Version        = "2.0.0-RC6"
  }

  lazy val cats: Modules = Seq(
    "org.typelevel" %% "cats-core"   % Versions.cats,
    "org.typelevel" %% "cats-effect" % Versions.catsEffect
  )

  lazy val decline: Modules = Seq(
    "com.monovore" %% "decline",
    "com.monovore" %% "decline-effect"
  ).map(_ % Versions.decline)

  lazy val sttp: Modules = Seq(
    "com.softwaremill.sttp.client3" %% "core",
    "com.softwaremill.sttp.client3" %% "circe",
    "com.softwaremill.sttp.client3" %% "async-http-client-backend-cats",
    "com.softwaremill.sttp.client3" %% "async-http-client-backend-fs2",
    "com.softwaremill.sttp.client3" %% "slf4j-backend"
  ).map(_ % Versions.sttp)

  lazy val circe: Modules = Seq(
    "io.circe" %% "circe-core",
    "io.circe" %% "circe-generic",
    "io.circe" %% "circe-parser",
    // "io.circe" %% "circe-optics", Not yet compatible with Scala 3 (Read: https://github.com/circe/circe-optics/issues/230)
    "io.circe" %% "circe-fs2"
  ).map(_ % Versions.circe)

  lazy val monocle: Modules = Seq(
    "dev.optics" %% "monocle-core",
    "dev.optics" %% "monocle-macro"
  ).map(_ % Versions.monocle)

  lazy val fs2: Modules = Seq(
    "co.fs2" %% "fs2-core",
    "co.fs2" %% "fs2-io",
    "co.fs2" %% "fs2-reactive-streams"
  ).map(_ % Versions.fs2)

  lazy val logging: Modules = Seq(
    "ch.qos.logback" % "logback-classic" % Versions.logback
  ) ++ Seq(
    "org.typelevel" %% "log4cats-core", // Only if you want to Support Any Backend
    "org.typelevel" %% "log4cats-slf4j" // Direct Slf4j Support - Recommended
  ).map(_ % Versions.log4cats)

  lazy val mongodb: Modules = Seq(
    "org.mongodb" % "mongodb-driver-sync" % "4.6.0"
  )

  val doobie: Seq[ModuleID] = Seq(
    "org.tpolecat" %% "doobie-core",
    "org.tpolecat" %% "doobie-postgres", // Postgres driver 42.3.1 + type mappings.
    "org.tpolecat" %% "doobie-postgres-circe",
    "org.tpolecat" %% "doobie-hikari"
  ).map(_            % Versions.doobie) ++ Seq(
    "org.postgresql" % "postgresql"  % "42.3.4",
    "org.flywaydb"   % "flyway-core" % "8.5.9",
    "com.zaxxer"     % "HikariCP"    % "5.0.1"
  )

  lazy val zio: Seq[ModuleID] = Seq(
    "dev.zio" %% "zio",
    "dev.zio" %% "zio-streams"
  ).map(_ % Versions.zio)

  lazy val json: Seq[ModuleID] = Seq(
    "dev.zio" %% "zio-json"      % "0.3.0-RC8",
    "dev.zio" %% "zio-json-yaml" % "0.3.0-RC8"
  )

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
