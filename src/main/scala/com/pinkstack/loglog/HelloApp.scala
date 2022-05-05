package com.pinkstack.loglog

import zio.*
import zio.ZIO.{acquireReleaseWith, attempt, attemptBlocking, fromEither, logInfo}
import zio.Console.{print, printLine}
import zio.json.*
import zio.json.yaml.*
import java.io.IOException
import java.net.{URI, URL}
import scala.io.BufferedSource
import scala.concurrent.duration
case class Channel(name: String, url: URL, enabled: Boolean)

object Channel {
  implicit val urlDecoder: JsonDecoder[URL]         = JsonDecoder[String].map(s => new URL(s))
  implicit val channelDecoder: JsonDecoder[Channel] = DeriveJsonDecoder.gen[Channel]
}

object HelloApp extends ZIOAppDefault:

  def updateChannels(channels: Vector[Channel]) =
    ZIO.foreachPar(channels)(fetchChannelStats).withParallelism(4)

  def fetchChannelStats(channel: Channel): Task[Unit] =
    (ZIO.attempt(println(s"Fetching ${channel}")) *> printLine("Done fetching")).delay(1.second)

  def activeChannels(): Task[Vector[Channel]] =
    for
      content <- Resources.read("channels.yml").orDie
      channels <- fromEither(content.fromYaml[Vector[Channel]])
        .map(_.filter(_.enabled))
        .mapError(e => new Throwable(e))
    yield channels

  def app =
    for
      _ <- logInfo("Booting...")
      v <- activeChannels().flatMap(updateChannels) repeat Schedule.forever && Schedule.spaced(2.seconds)
      _ <- ZIO.debug(v)
    yield ()

  def run: ZIO[Any, Throwable, Unit] = app
