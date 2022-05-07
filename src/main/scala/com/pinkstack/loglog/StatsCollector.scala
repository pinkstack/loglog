package com.pinkstack.loglog

import com.influxdb.client.domain.WritePrecision
import com.influxdb.client.write.Point
import io.circe.parser.parse as circeParse
import io.circe.{Json, ParsingFailure}
import org.asynchttpclient.Dsl.{asyncHttpClient, get, post}
import org.asynchttpclient.{AsyncHttpClient, ListenableFuture, Request, Response}
import zio.Console.{print, printLine}
import zio.ZIO.{acquireRelease, acquireReleaseWith, attempt, attemptBlocking, fromEither, logInfo, whenCase}
import zio.json.*
import zio.json.yaml.*
import zio.*
import zio.Queue

import java.io.IOException
import java.net.{URI, URL}
import java.time.Instant
import scala.concurrent.{duration, ExecutionContext, Future, Promise}
import scala.io.BufferedSource

object StatsCollector:
  type Measurements = Queue[ChannelMeasurement]

  private val readCount: Json => Option[Int] =
    _.hcursor.downField("response").downField("concurrent").get[Int]("cnt").toOption

  private def fetchChannelStats(
      measurements: Measurements
  )(channel: Channel): ZIO[HttpClient with InfluxDB, Throwable, Unit] =
    for
      body     <- HttpClient.executeRequest(get(channel.url).build())
      countOpt <- fromEither(circeParse(body)).map(readCount)
      _ <- whenCase(countOpt) {
        case None => ZIO.fail("Sorry, no data was collected.").unit
        case Some(count: Int) =>
          measurements.offer(ChannelMeasurement(channel, count))
      }.mapError(e => new Exception(e))
    /*
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
     */
    yield ()

  private val activeChannels: Task[Channels] =
    for
      content  <- Resources.read("channels.yml").orDie
      channels <- fromEither(content.fromYaml[Channels]).mapError(e => new Throwable(e))
    yield channels.filter(_.enabled)

  private def updateChannels(measurements: Measurements)(
      channels: Channels
  ): ZIO[HttpClient with InfluxDB, Throwable, Vector[Unit]] =
    ZIO.foreachPar(channels)(fetchChannelStats(measurements)).withParallelism(4)

  def collectAntPublish(measurements: Measurements): RIO[HttpClient with InfluxDB, Vector[Unit]] =
    activeChannels.flatMap(updateChannels(measurements))
