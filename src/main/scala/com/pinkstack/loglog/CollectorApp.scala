package com.pinkstack.loglog

import com.influxdb.client.domain.WritePrecision
import io.circe.parser.parse as circeParse
import io.circe.{Json, ParsingFailure}
import org.asynchttpclient.Dsl.{asyncHttpClient, get, post}
import org.asynchttpclient.{AsyncHttpClient, ListenableFuture, Request, Response}
import zio.*
import zio.Console.{print, printLine}
import zio.ZIO.{acquireRelease, acquireReleaseWith, attempt, attemptBlocking, fromEither, logInfo}
import zio.json.*
import zio.json.yaml.*
import com.influxdb.client.write.Point

import java.io.IOException
import java.net.{URI, URL}
import java.time.Instant
import scala.concurrent.{duration, ExecutionContext, Future, Promise}
import scala.io.BufferedSource

given Conversion[URL, String] with
  override def apply(url: URL): String = url.toString

case class Channel(name: String, url: URL, enabled: Boolean)

object Channel {
  implicit val urlDecoder: JsonDecoder[URL]         = JsonDecoder[String].map(s => new URL(s))
  implicit val channelDecoder: JsonDecoder[Channel] = DeriveJsonDecoder.gen[Channel]
}

type Channels = Vector[Channel]

object CollectorApp extends ZIOAppDefault:

  def updateChannels(channels: Vector[Channel]): ZIO[HttpClient with InfluxDB, Throwable, Vector[Unit]] =
    ZIO.foreachPar(channels)(fetchChannelStats).withParallelism(4)

  val readCount: Json => Option[Int] =
    _.hcursor.downField("response").downField("concurrent").get[Int]("cnt").toOption

  val patchUrl: URL => String =
    _.toString.replace("https://api.rtvslo.si", "http://localhost:7070")

  def fetchChannelStats(channel: Channel): ZIO[HttpClient with InfluxDB, Throwable, Unit] =
    for
      body               <- HttpClient.executeRequest(get(channel.url).build())
      count: Option[Int] <- fromEither(circeParse(body)).map(readCount)
      _ <- ZIO
        .when(count.isDefined) {
          printLine(s"${channel.name}: ${count.get}") *>
            InfluxDB
              .write(
                Point
                  .measurement("concurrent_viewers")
                  .addTag("channel", channel.name)
                  .addTag("media", if (channel.name.contains("tv")) "tv" else "ra")
                  .addField("count", count.get)
                  .time(Instant.now().toEpochMilli, WritePrecision.MS)
              )
        }
    yield ()

  val activeChannels: Task[Channels] =
    for
      content  <- Resources.read("channels.yml").orDie
      channels <- fromEither(content.fromYaml[Channels]).mapError(e => new Throwable(e))
    yield channels.filter(_.enabled)

  def collectStats: RIO[HttpClient with InfluxDB, Vector[Unit]] =
    activeChannels.flatMap(updateChannels)

  def app: ZIO[HttpClient with InfluxDB, Throwable, Unit] =
    for
      _ <- logInfo("Booting...")
      f <- collectStats.repeat(Schedule.spaced(10.seconds)).fork
      _ <- logInfo("Booted.")
      _ <- f.join
    yield ()

  def influxConfig(): ZIO[Any, String, InfluxDBLive.InfluxConfig] =
    for
      url <- ZIO
        .attempt(sys.env.getOrElse("INFLUXDB_URL", "http://0.0.0.0:8086"))
        .map(new URL(_))
        .catchAll(_ => ZIO.fail("Wot?"))
      token  <- ZIO.fromOption(sys.env.get("INFLUXDB_ADMIN_USER_TOKEN")).catchAll(_ => ZIO.fail("Missing \"token\""))
      org    <- ZIO.fromOption(sys.env.get("INFLUXDB_ORG")).catchAll(_ => ZIO.fail("Missing \"org\""))
      bucket <- ZIO.fromOption(sys.env.get("INFLUXDB_BUCKET")).catchAll(_ => ZIO.fail("Missing \"bucket\""))
    yield (url, token, org, bucket)

  def run: ZIO[Any, Throwable, Unit] =
    influxConfig()
      .flatMap { influx =>
        app.provideLayer(HttpClientLive.layer ++ InfluxDBLive.layer(influx))
      }
      .mapError(e => new Exception(s"eee ${e}"))
