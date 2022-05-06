package com.pinkstack.loglog

import io.circe.parser.parse as circeParse
import io.circe.{Json, ParsingFailure}
import org.asynchttpclient.Dsl.{asyncHttpClient, get, post}
import org.asynchttpclient.{AsyncHttpClient, ListenableFuture, Request, Response}
import zio.*
import zio.Console.{print, printLine}
import zio.ZIO.{acquireRelease, acquireReleaseWith, attempt, attemptBlocking, fromEither, logInfo}
import zio.json.*
import zio.json.yaml.*

import java.io.IOException
import java.net.{URI, URL}
import scala.concurrent.{ExecutionContext, Future, Promise, duration}
import scala.io.BufferedSource

case class Channel(name: String, url: URL, enabled: Boolean)

object Channel {
  implicit val urlDecoder: JsonDecoder[URL]         = JsonDecoder[String].map(s => new URL(s))
  implicit val channelDecoder: JsonDecoder[Channel] = DeriveJsonDecoder.gen[Channel]
}

type Channels = Vector[Channel]

object HelloApp extends ZIOAppDefault:

  def updateChannels(channels: Vector[Channel]): ZIO[HttpClient, Throwable, Vector[Unit]] =
    ZIO.foreachPar(channels)(fetchChannelStats).withParallelism(4)

  val readCount: Json => Option[Int] =
    _.hcursor.downField("response").downField("concurrent").get[Int]("cnt").toOption

  def fetchChannelStats(channel: Channel): ZIO[HttpClient, Throwable, Unit] =
    for
      body               <- HttpClient.executeRequest(get(channel.url.toString).build())
      count: Option[Int] <- fromEither(circeParse(body)).map(readCount)
      _ <- ZIO
        .when(count.isDefined)(printLine(s"${channel.name}: ${count.get}"))
    yield ()

  def activeChannels(): Task[Channels] =
    for
      content  <- Resources.read("channels.yml").orDie
      channels <- fromEither(content.fromYaml[Channels]).mapError(e => new Throwable(e))
    yield channels.filter(_.enabled)

  def app: ZIO[HttpClient, Throwable, Unit] =
    for
      _ <- logInfo("Booting...")
      _ <- activeChannels().flatMap(updateChannels) repeat Schedule.forever && Schedule.spaced(2.seconds)
    yield ()

  def run: ZIO[Any, Throwable, Unit] = app
    .provideLayer(HttpClientLive.layer)
