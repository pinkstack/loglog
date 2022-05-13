package com.pinkstack.loglog

import io.circe.parser.parse as circeParse
import io.circe.{Json, ParsingFailure}
import org.asynchttpclient.Dsl.{asyncHttpClient, get, post}
import org.asynchttpclient.{AsyncHttpClient, ListenableFuture, Request, Response}
import zio.Console.{print, printLine}
import zio.ZIO.{acquireRelease, acquireReleaseWith, attempt, attemptBlocking, foreachPar, fromEither, logInfo, whenCase}
import zio.json.*
import zio.json.yaml.*
import zio.*
import zio.Queue

import java.io.IOException
import java.net.{URI, URL}
import java.time.Instant
import scala.concurrent.{duration, ExecutionContext, Future, Promise}

object ViewershipCollector:
  type Measurements = Queue[ChannelMeasurement]

  private val readCount: Json => Option[Int] =
    _.hcursor.downField("response").downField("concurrent").get[Int]("cnt").toOption

  private def fetchChannelStats(measurements: Measurements, channel: Channel): RIO[HttpClient, Unit] =
    for
      body     <- HttpClient.execute(get(channel.url).build())
      countOpt <- fromEither(circeParse(body)).map(readCount)
      _ <- whenCase(countOpt) {
        case None             => ZIO.fail("Sorry, no data.").unit
        case Some(count: Int) => measurements.offer(ChannelMeasurement(channel, count))
      }.mapError(e => new Exception(e))
    yield ()

  private val patchTraffic: Channel => Channel = channel =>
    sys.env
      .get("PATCH_API_URL")
      .map(replacement => channel.copy(url = channel.url.toString.replace("https://api.rtvslo.si", replacement)))
      .getOrElse(channel)

  private val activeChannels: Task[Channels] =
    for
      content  <- Resources.read("channels.yml").orDie
      channels <- fromEither(content.fromYaml[Channels]).mapError(e => new Throwable(e))
      filteredChannels = channels.filter(_.enabled).map(patchTraffic)
    yield filteredChannels

  private def updateChannels(measurements: Measurements)(
      channels: Channels
  ): ZIO[HttpClient, Throwable, Vector[Unit]] =
    foreachPar(channels) { channel =>
      fetchChannelStats(measurements, channel)
        .catchSome { case ex: java.util.concurrent.TimeoutException =>
          ZIO.logWarning(s"Caught timeout exception ${ex.getMessage}")
        }
    }.withParallelism(4)

  val collectAndOffer: Measurements => RIO[HttpClient, Vector[Unit]] = measurements =>
    activeChannels
      .flatMap(updateChannels(measurements))
