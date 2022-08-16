package com.pinkstack.loglog

import api.CoralogixLogger
import entities.Severity
import zio.{Task, UIO, URIO, ZIO, ZLayer}
import zio.ZIO.{attempt, fail, fromOption, serviceWithZIO, succeed}

trait CoraLogger:
  def log(message: String, severity: Severity = Severity.INFO): UIO[Unit]
  def critical(message: String): UIO[Unit]
  def info(message: String): UIO[Unit]

object CoraLogger:
  def log(message: String, severity: Severity = Severity.INFO): URIO[CoraLogger, Unit] =
    serviceWithZIO[CoraLogger](_.log(message, severity))

  def critical(message: String): URIO[CoraLogger, Unit] =
    serviceWithZIO[CoraLogger](_.critical(message))

  def info(message: String): URIO[CoraLogger, Unit] =
    serviceWithZIO[CoraLogger](_.info(message))

case class CoraLoggerLive(coralogixLogger: CoralogixLogger) extends CoraLogger:
  override def log(message: String, severity: Severity = Severity.INFO): UIO[Unit] =
    succeed(coralogixLogger.log(severity, message))

  override def critical(message: String): UIO[Unit] =
    succeed(coralogixLogger.critical(message))

  override def info(message: String): UIO[Unit] =
    succeed(coralogixLogger.info(message))

object CoraLoggerLive:
  private def buildLayer: Task[CoraLoggerLive] =
    for
      loglogEnv      <- zio.System.envOrElse("LOGLOG_ENV", "development")
      readPrivateKey <- zio.System.env("CORALOGIX_PRIVATE_KEY")
      privateKey     <- fromOption(readPrivateKey).orDieWith(_ =>
        new RuntimeException("\"CORALOGIX_PRIVATE_KEY\" is not set.")
      )
      _              <- succeed(CoralogixLogger.setDebugMode(false))
      _              <- succeed(CoralogixLogger.configure(privateKey, "loglog", s"${loglogEnv}-system"))
      resource       <- succeed(CoraLoggerLive.apply(new CoralogixLogger("CoraLogger")))
    yield resource

  val layer: ZLayer[Any, Throwable, CoraLogger] =
    ZLayer.fromZIO(buildLayer.orDieWith(e => new RuntimeException(s"Boom with ${e}")))
