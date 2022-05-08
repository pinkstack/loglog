package com.pinkstack.loglog

import org.asynchttpclient.Dsl.asyncHttpClient
import org.asynchttpclient.{AsyncHttpClient, AsyncHttpClientConfig, Request, Response}
import zio.ZIO.{acquireRelease, attempt, fromFuture, fromFutureJava, serviceWithZIO}
import zio.{RIO, Task, UIO, URIO, ZIO, ZLayer}

trait HttpClient:
  def execute(request: Request): Task[String]
  def close(): UIO[Unit]

object HttpClient:
  def execute(request: Request): RIO[HttpClient, String] =
    serviceWithZIO[HttpClient](_.execute(request))
  def close(): URIO[HttpClient, Unit] = serviceWithZIO[HttpClient](_.close())

case class HttpClientLive(asyncHttpClient: AsyncHttpClient) extends HttpClient:
  def execute(request: Request): Task[String] =
    fromFutureJava(asyncHttpClient.executeRequest(request)).map(_.getResponseBody)

  def close(): UIO[Unit] =
    attempt(asyncHttpClient.close()).orDie.debug("AsyncHttpClient closed.")

object HttpClientLive:
  private val defineLayer: AsyncHttpClient => ZLayer[Any, Throwable, HttpClient] = client =>
    ZLayer.scoped {
      acquireRelease(attempt(HttpClientLive(client)).debug("Booted."))(_.close())
    }

  val layer: ZLayer[Any, Throwable, HttpClient] =
    defineLayer(asyncHttpClient())

  def layer(config: AsyncHttpClientConfig): ZLayer[Any, Throwable, HttpClient] =
    defineLayer(asyncHttpClient(config))
