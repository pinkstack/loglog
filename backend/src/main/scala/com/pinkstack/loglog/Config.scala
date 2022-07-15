package com.pinkstack.loglog

import com.typesafe.config.{ConfigException, ConfigFactory}
import zio.{IO, ZIO}

import java.net.URL
import scala.util.Try

object Config:
  final case class MeasurementsConfig(queueCapacity: Int)
  final case class InfluxConfig(url: URL, token: String, org: String, bucket: String)
  final case class HttpClientConfig(patchApiUrl: String, connectTimeout: Int, readTimeout: Int)
  final case class AppConfig(measurements: MeasurementsConfig, influx: InfluxConfig, httpClient: HttpClientConfig)

  def load: Try[AppConfig] =
    for
      config           <- Try(ConfigFactory.load())
      influxConfig     <- Try(config.getConfig("influxdb"))
      url              <- Try(influxConfig.getString("url")).flatMap(s => Try(new URL(s)))
      token            <- Try(influxConfig.getString("token"))
      org              <- Try(influxConfig.getString("org"))
      bucket           <- Try(influxConfig.getString("bucket"))
      httpClientConfig <- Try(config.getConfig("httpclient"))
      patchApiUrl      <- Try(httpClientConfig.getString("patch-api-url"))
      connectTimeout   <- Try(httpClientConfig.getInt("connect-timeout"))
      readTimeout      <- Try(httpClientConfig.getInt("read-timeout"))
      measurements     <- Try(config.getConfig("measurements"))
      queueCapacity    <- Try(measurements.getInt("queue-capacity"))
    yield AppConfig.apply(
      MeasurementsConfig(queueCapacity),
      InfluxConfig(url, token, org, bucket),
      HttpClientConfig(patchApiUrl, connectTimeout, readTimeout)
    )
