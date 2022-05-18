package com.pinkstack.loglog

import com.influxdb.client.write.Point
import com.influxdb.client.{InfluxDBClient, InfluxDBClientFactory}
import com.pinkstack.loglog.Config.InfluxConfig
import zio.ZIO.{acquireRelease, attempt, fromOption, service, serviceWithZIO}
import zio.{RIO, Task, UIO, URIO, ZIO, ZLayer}

import java.net.URL
import scala.jdk.CollectionConverters.*

trait InfluxDB {
  def write(point: Point): Task[Boolean]
  def write(points: List[Point]): Task[Boolean]
  def close(): UIO[Unit]
}

object InfluxDB {
  def write(point: Point): RIO[InfluxDB, Boolean] =
    serviceWithZIO[InfluxDB](_.write(point))

  def write(points: List[Point]): RIO[InfluxDB, Boolean] =
    serviceWithZIO[InfluxDB](_.write(points))

  def close(): URIO[InfluxDB, Unit] =
    serviceWithZIO[InfluxDB](_.close())
}

case class InfluxDBLive(client: InfluxDBClient) extends InfluxDB {
  private val writeApi = client.getWriteApiBlocking

  override def write(point: Point): Task[Boolean] =
    attempt(writeApi.writePoint(point)).orDie.map(_ => true)

  override def write(points: List[Point]): Task[Boolean] =
    attempt(writeApi.writePoints(points.asJava)).map(_ => true)

  override def close(): UIO[Unit] =
    attempt(client.close()).orDie.debug("InfluxDB closed.")
}

object InfluxDBLive {
  val layer: ZLayer[Config.AppConfig, Throwable, InfluxDB] =
    ZLayer.scoped(
      service[Config.AppConfig]
        .map(_.influx)
        .flatMap { case InfluxConfig(url, token, org, bucket) =>
          acquireRelease(
            attempt(
              InfluxDBLive(InfluxDBClientFactory.create(url.toString, token.toCharArray, org, bucket))
            ).debug("Booted.")
          )(_.close())
        }
    )
}
