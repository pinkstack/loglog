package com.pinkstack.loglog

import com.pinkstack.loglog.Config.AppConfig
import com.typesafe.config.{ConfigException, ConfigFactory}
import org.asynchttpclient.Dsl.config as asyncHttpClientConfig
import zio.Schedule.spaced
import zio.ZIO.{logInfo, service}
import zio.ZLayer.fromZIO
import zio.{durationInt, Queue, Schedule, ZIO, ZIOAppDefault, ZLayer}

object CollectorApp extends ZIOAppDefault:
  def program: ZIO[Config.AppConfig & HttpClient & InfluxDB, Throwable, Unit] =
    for
      config       <- service[Config.AppConfig]
      _            <- logInfo(s"Booting,... 🐇")
      measurements <- Queue.sliding[ChannelMeasurement](config.measurements.queueCapacity)
      _            <- ViewershipCollector
        .collectAndOffer(measurements)
        .repeat(spaced(10.seconds))
        .raceFirst(
          Pusher.observeAndPush(
            measurements
          )
        )
    yield ()

  def run =
    program.provideLayer(
      Config.live ++ (Config.live >+> HttpClientLive.layer) ++ (Config.live >+> InfluxDBLive.layer)
    )
