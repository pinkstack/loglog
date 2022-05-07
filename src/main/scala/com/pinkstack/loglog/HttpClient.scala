package com.pinkstack.loglog

import org.asynchttpclient.Dsl.asyncHttpClient
import org.asynchttpclient.{AsyncHttpClient, Request}
import zio.ZIO.{acquireRelease, attempt, fromFuture, serviceWithZIO}
import zio.{RIO, Task, UIO, URIO, ZIO, ZLayer}

import scala.concurrent.{Future, Promise}

trait HttpClient {
  def executeRequest(request: Request): Task[String]
  def close(): UIO[Unit]
}

object HttpClient {
  def executeRequest(request: Request): RIO[HttpClient, String] =
    serviceWithZIO[HttpClient](_.executeRequest(request))

  def close(): URIO[HttpClient, Unit] = serviceWithZIO[HttpClient](_.close())
}

case class HttpClientLive(asyncHttpClient: AsyncHttpClient) extends HttpClient {
  private def buildRequestFuture(request: Request): Future[String] =
    val promise          = Promise[String]
    val listenableFuture = asyncHttpClient.executeRequest(request)
    listenableFuture.addListener(() => promise.success(listenableFuture.get().getResponseBody), null)
    promise.future

  def executeRequest(request: Request): Task[String] =
    fromFuture(implicit executionContext => buildRequestFuture(request))
      .tapError(error => ZIO.debug(s"Got error ${error}"))

  def close(): UIO[Unit] =
    attempt(asyncHttpClient.close()).orDie.debug("AsyncHttpClient closed.")
}

object HttpClientLive {
  val layer: ZLayer[Any, Throwable, HttpClient] = ZLayer.scoped {
    acquireRelease(
      attempt(HttpClientLive(asyncHttpClient())).debug("Booted.")
    )(client => client.close())
  }
}
