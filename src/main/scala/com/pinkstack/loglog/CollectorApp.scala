package com.pinkstack.loglog

import org.asynchttpclient.Dsl.config as asyncHttpClientConfig
import zio.Schedule.spaced
import zio.ZIO.logInfo
import zio.{Queue, Schedule, ZIO, ZIOAppDefault, durationInt}

object CollectorApp extends ZIOAppDefault:
  def app: ZIO[HttpClient with InfluxDB, Throwable, Unit] =
    for
      _            <- logInfo("Booting. 🐇")
      measurements <- Queue.sliding[ChannelMeasurement](200)
      _ <- ViewershipCollector
        .collectAndOffer(measurements)
        .repeat(spaced(10.seconds))
        .raceFirst(Pusher.observeAndPush(measurements))
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
