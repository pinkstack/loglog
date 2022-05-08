package com.pinkstack.loglog

import org.asynchttpclient.Dsl.config as asyncHttpClientConfig
import zio.{durationInt, Queue, Schedule, ZIO, ZIOAppDefault}
import zio.ZIO.{acquireRelease, acquireReleaseWith, attempt, attemptBlocking, fromEither, logInfo}
import zio.json.*
import zio.json.yaml.*

import java.net.{URI, URL}

given Conversion[URL, String] with
  override def apply(url: URL): String = url.toString

object CollectorApp extends ZIOAppDefault:
  def app: ZIO[HttpClient with InfluxDB, Throwable, Unit] =
    for
      _            <- logInfo("Booting...")
      measurements <- Queue.sliding[ChannelMeasurement](100)
      collection <- StatsCollector
        .collectAndOffer(measurements)
        .repeat(Schedule.spaced(10.seconds))
        .fork
      _ <- logInfo("Booted.")
      _ <- collection.join
    yield ()

  def run: ZIO[Any, Throwable, Unit] =
    InfluxDBLive.readConfig
      .flatMap(influxConfig =>
        app.provideLayer(
          HttpClientLive.layer(
            asyncHttpClientConfig
              .setConnectTimeout(2000)
              .setReadTimeout(2000)
              .build()
          ) ++ InfluxDBLive.layer(influxConfig)
        )
      )
      .mapError(e => new Exception(s"System crashed with ${e}"))
