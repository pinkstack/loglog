package com.pinkstack.loglog

import api.CoralogixLogger
import com.pinkstack.loglog.Config.AppConfig
import com.typesafe.config.{ConfigException, ConfigFactory}
import entities.Severity
import org.asynchttpclient.Dsl.config as asyncHttpClientConfig
import zio.Schedule.spaced
import zio.ZIO.{acquireRelease, logInfo, service, serviceWithZIO}
import zio.Task
import zio.ZLayer.fromZIO
import zio.{durationInt, Queue, Schedule, ZIO, ZIOAppDefault, ZLayer}

object CollectorApp extends ZIOAppDefault:
  def program: ZIO[Config.AppConfig & HttpClient & InfluxDB & CoraLogger, Throwable, Unit] =
    for
      logger       <- service[CoraLogger]
      config       <- service[Config.AppConfig]
      loglogEnv    <- zio.System.envOrElse("LOGLOG_ENV", "development")
      _            <- logInfo(s"Booting,... 🐇 in ${loglogEnv}")
      _            <- logger.log(s"Booting loglog in ${loglogEnv}")
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
      Config.live ++ (Config.live >+> HttpClientLive.layer) ++ (Config.live >+> InfluxDBLive.layer) ++ CoraLoggerLive.layer
    )
