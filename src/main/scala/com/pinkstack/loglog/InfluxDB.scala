package com.pinkstack.loglog

import com.influxdb.client.write.Point
import com.influxdb.client.{InfluxDBClient, InfluxDBClientFactory}
import zio.ZIO.{acquireRelease, attempt, fromOption, serviceWithZIO}
import zio.{RIO, Task, UIO, URIO, ZIO, ZLayer}
import scala.jdk.CollectionConverters.*
import java.net.URL

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
  type Token        = String
  type Org          = String
  type Bucket       = String
  type InfluxConfig = (URL, Token, Org, Bucket)

  val layer: InfluxConfig => ZLayer[Any, Throwable, InfluxDB] = (url, token, org, bucket) =>
    ZLayer.scoped {
      acquireRelease(
        attempt(InfluxDBLive(InfluxDBClientFactory.create(url.toString, token.toCharArray, org, bucket)))
          .debug("Booted.")
      )(_.close())
    }

  private def readEnv(key: String, default: Option[String] = None) =
    fromOption(default.map(d => sys.env.getOrElse(key, d)).orElse(sys.env.get(key)))
      .catchAll(_ => ZIO.fail(s"Missing environment variable \"${key}\""))

  val readConfig: ZIO[Any, String, InfluxDBLive.InfluxConfig] =
    for
      url    <- readEnv("INFLUXDB_URL", Some("http://0.0.0.0:8086")).map(new URL(_))
      token  <- readEnv("INFLUXDB_TOKEN").orElse(readEnv("INFLUXDB_ADMIN_USER_TOKEN"))
      org    <- readEnv("INFLUXDB_ORG")
      bucket <- readEnv("INFLUXDB_BUCKET")
    yield (url, token, org, bucket)
}
