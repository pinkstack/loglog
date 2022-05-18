package com.pinkstack.loglog

import org.asynchttpclient.Dsl.config as asyncHttpClientConfig
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
  private val clientConfig = ZIO.service[Config.AppConfig].map(_.httpClient)

  private val mkClient: ZIO[Config.AppConfig & zio.Scope, Throwable, HttpClientLive] =
    clientConfig.flatMap(clientConfig =>
      val client = asyncHttpClient(
        asyncHttpClientConfig()
          .setConnectTimeout(clientConfig.connectTimeout)
          .setReadTimeout(clientConfig.readTimeout)
          .build()
      )
      acquireRelease(attempt(HttpClientLive(client)).debug("Booted."))(_.close())
    )

  val layer: ZLayer[Config.AppConfig, Throwable, HttpClient] = ZLayer.scoped(mkClient)
