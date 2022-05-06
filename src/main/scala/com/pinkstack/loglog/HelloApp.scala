package com.pinkstack.loglog

import zio.*
import zio.ZIO.{acquireRelease, acquireReleaseWith, attempt, attemptBlocking, fromEither, logInfo}
import zio.Console.{print, printLine}
import zio.json.*
import zio.json.yaml.*

import java.io.IOException
import java.net.{URI, URL}
import scala.io.BufferedSource
import scala.concurrent.{duration, ExecutionContext, Future, Promise}
import org.asynchttpclient.{AsyncHttpClient, ListenableFuture, Request, Response}
import org.asynchttpclient.Dsl.{asyncHttpClient, get, post}

case class Channel(name: String, url: URL, enabled: Boolean)

object Channel {
  implicit val urlDecoder: JsonDecoder[URL]         = JsonDecoder[String].map(s => new URL(s))
  implicit val channelDecoder: JsonDecoder[Channel] = DeriveJsonDecoder.gen[Channel]
}

type Channels = Vector[Channel]

object HelloApp extends ZIOAppDefault:

  def updateChannels(channels: Vector[Channel]): Task[Vector[Unit]] =
    ZIO.foreachPar(channels)(fetchChannelStats).withParallelism(4)

  def fetchChannelStats(channel: Channel): Task[Unit] =
    (ZIO.attempt(println(s"Fetching ${channel}")) *> printLine("Done fetching")).delay(1.second)

  def activeChannels(): Task[Channels] =
    for
      content  <- Resources.read("channels.yml").orDie
      channels <- fromEither(content.fromYaml[Channels]).mapError(e => new Throwable(e))
    yield channels.filter(_.enabled)

  def app: ZIO[HttpClient, Throwable, Unit] =
    for
      _       <- logInfo("Booting...")
      backend <- HttpClient.executeRequest(get("http://icanhazip.com/").build())
      _       <- ZIO.debug(s"Response is \"${backend}\"")
      _       <- ZIO.debug("Done...")
    yield ()

  def run: ZIO[Any, Throwable, Unit] = app
    .provideLayer(HttpClientLive.layer)
