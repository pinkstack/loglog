package com.pinkstack.loglog

import zio.{Clock, UIO, URIO, URLayer, ZIO, ZLayer}

trait MyLogging {
  def log(line: String): UIO[Unit]
}

object MyLogging {
  def log(line: String): URIO[MyLogging, Unit] = ZIO.serviceWithZIO[MyLogging](_.log(line))
}

case class MyLoggingLive(clock: Clock) extends MyLogging {
  override def log(line: String): UIO[Unit] =
    for
      t <- clock.currentDateTime
      _ <- ZIO.succeed(println(s"ML at ${t}: ${line}"))
    yield ()
}

object MyLoggingLive {

  val layer: ZLayer[Any, Nothing, MyLogging] = ZLayer {
    for {
      clock <- ZIO.clock
    } yield MyLoggingLive(clock)
  }
}
