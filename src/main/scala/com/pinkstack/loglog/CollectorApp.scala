package com.pinkstack.loglog

import org.asynchttpclient.Dsl.config as asyncHttpClientConfig
import zio.{durationInt, Queue, RIO, Schedule, URIO, ZIO, ZIOAppDefault}
import zio.ZIO.logInfo

object CollectorApp extends ZIOAppDefault:
  def app: ZIO[HttpClient with InfluxDB, Throwable, Unit] =
    for
      _            <- logInfo("Booting... 🐇")
      measurements <- Queue.sliding[ChannelMeasurement](200)
      collection   <- StatsCollector.collectAndOffer(measurements).repeat(Schedule.spaced(10.seconds)).fork
      pushing      <- StatsPusher.observeAndPush(measurements).fork
      _            <- collection.join
      _            <- pushing.join
      _            <- logInfo("Booted. ✅")
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
