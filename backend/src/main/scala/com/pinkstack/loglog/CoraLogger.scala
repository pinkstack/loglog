package com.pinkstack.loglog

import api.CoralogixLogger
import entities.Severity
import io.opentelemetry.api.metrics.MeterProvider
import io.opentelemetry.exporter.otlp.metrics.OtlpGrpcMetricExporter
import io.opentelemetry.sdk.metrics.SdkMeterProvider
import io.opentelemetry.sdk.metrics.`export`.PeriodicMetricReader
import zio.{Task, ZIO, ZLayer}
import zio.ZIO.{attempt, fail, fromOption, serviceWithZIO, succeed}

trait CoraLogger:
  def log(message: String, severity: Severity = Severity.INFO): Task[Unit]
  def critical(message: String): Task[Unit]
  def counter(meter: String, counterName: String, value: Long): Task[Unit]

object CoraLogger:
  def log(message: String, severity: Severity = Severity.INFO) =
    serviceWithZIO[CoraLogger](_.log(message, severity))

  def critical(message: String) =
    serviceWithZIO[CoraLogger](_.critical(message))

  def counter(meter: String, counterName: String, value: Long) =
    serviceWithZIO[CoraLogger](_.counter(meter, counterName, value))

case class CoraLoggerLive(coralogixLogger: CoralogixLogger, meterProvider: SdkMeterProvider) extends CoraLogger:
  def log(message: String, severity: Severity = Severity.INFO): Task[Unit] =
    succeed(coralogixLogger.log(severity, message))

  def critical(message: String): Task[Unit] =
    succeed(coralogixLogger.critical(message))

  def counter(meter: String, counterName: String, value: Long) =
    for
      meter   <- succeed(meterProvider.meterBuilder(meter).build())
      counter <- succeed(meter.counterBuilder(counterName).build())
      _       <- succeed(counter.add(value)) *> succeed(meterProvider.forceFlush())
    yield ()

object CoraLoggerLive:
  private def buildLayer: Task[CoraLoggerLive] =
    for
      readPrivateKey  <- zio.System.env("CORALOGIX_PRIVATE_KEY")
      privateKey      <- fromOption(readPrivateKey).orDieWith(_ =>
        new RuntimeException("\"CORALOGIX_PRIVATE_KEY\" is not set.")
      )
      _               <- succeed(CoralogixLogger.configure(privateKey, "loglog", "dev-system"))
      metricsEndpoint <- succeed("https://prometheus-gateway.coralogix.us/prometheus/api/v1/write")
      meterProvider   <- attempt(
        SdkMeterProvider
          .builder()
          .registerMetricReader(
            PeriodicMetricReader
              .builder(
                OtlpGrpcMetricExporter
                  .builder()
                  .setEndpoint(metricsEndpoint)
                  .addHeader("Authorization", "Bearer " + privateKey)
                  .build()
              )
              .build()
          )
          .build()
      )
      resource        <- succeed(CoraLoggerLive(new CoralogixLogger("CoraLogger"), meterProvider))
    yield resource

  val layer: ZLayer[Any, Throwable, CoraLogger] =
    ZLayer.fromZIO(buildLayer.orDieWith(e => new RuntimeException(s"Boom with ${e}")))
