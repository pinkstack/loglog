package com.pinkstack.loglog

import com.pinkstack.loglog.Config.AppConfig
import com.typesafe.config.{ConfigException, ConfigFactory}
import org.asynchttpclient.Dsl.config as asyncHttpClientConfig
import zio.Schedule.spaced
import zio.ZIO.{acquireRelease, logInfo, service, serviceWithZIO}
import zio.Task
import zio.ZLayer.fromZIO
import zio.{durationInt, Queue, Schedule, ZIO, ZIOAppDefault, ZLayer}

object CollectorApp extends ZIOAppDefault:
  private def program: ZIO[Config.AppConfig & HttpClient & InfluxDB, Throwable, Unit] =
    for
      config       <- service[Config.AppConfig]
      loglogEnv    <- zio.System.envOrElse("LOGLOG_ENV", "development")
      _            <- logInfo(s"Booting,... 🐇 in ${loglogEnv}")
      measurements <- Queue.sliding[ChannelMeasurement](config.measurements.queueCapacity)
      _            <- ViewershipCollector
        .collectAndOffer(measurements)
        .repeat(spaced(10.seconds))
        .raceFirst(
          Pusher.observePushAndLog(
            measurements
          )
        )
    yield ()

  def run =
    program.provideLayer(
      Config.live ++ (Config.live >+> HttpClientLive.layer) ++ (Config.live >+> InfluxDBLive.layer)
    )
