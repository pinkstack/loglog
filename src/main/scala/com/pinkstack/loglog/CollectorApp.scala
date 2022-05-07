package com.pinkstack.loglog

import com.influxdb.client.domain.WritePrecision
import com.influxdb.client.write.Point
import io.circe.parser.parse as circeParse
import io.circe.{Json, ParsingFailure}
import org.asynchttpclient.Dsl.{asyncHttpClient, get, post}
import org.asynchttpclient.{AsyncHttpClient, ListenableFuture, Request, Response}
import zio.*
import zio.Console.{print, printLine}
import zio.ZIO.{acquireRelease, acquireReleaseWith, attempt, attemptBlocking, fromEither, logInfo}
import zio.json.*
import zio.json.yaml.*
import zio.Queue

import java.io.IOException
import java.net.{URI, URL}
import java.time.Instant
import scala.concurrent.{duration, ExecutionContext, Future, Promise}
import scala.io.BufferedSource

given Conversion[URL, String] with
  override def apply(url: URL): String = url.toString

object CollectorApp extends ZIOAppDefault:
  def app: ZIO[HttpClient with InfluxDB, Throwable, Unit] =
    for
      _            <- logInfo("Booting...")
      measurements <- Queue.sliding[ChannelMeasurement](100)
      collection   <- StatsCollector.collectAntPublish(measurements).repeat(Schedule.spaced(10.seconds)).fork
      _            <- logInfo("Booted.")
      _            <- collection.join
    yield ()

  def run: ZIO[Any, Throwable, Unit] =
    InfluxDBLive.readConfig
      .flatMap(influxConfig => app.provideLayer(HttpClientLive.layer ++ InfluxDBLive.layer(influxConfig)))
      .mapError(e => new Exception(s"Crashed with ${e}"))
