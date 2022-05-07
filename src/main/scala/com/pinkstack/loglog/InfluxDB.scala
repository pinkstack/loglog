package com.pinkstack.loglog

import com.influxdb.client.{InfluxDBClient, InfluxDBClientFactory}
import zio.{RIO, Task, UIO, URIO, ZIO, ZLayer}
import zio.ZIO.{acquireRelease, attempt, serviceWithZIO}
import com.influxdb.client.write.Point

import java.net.URL

trait InfluxDB {
  def write(point: Point): Task[Unit]
  def close(): UIO[Unit]
}

object InfluxDB {
  def write(point: Point): RIO[InfluxDB, Unit] =
    serviceWithZIO[InfluxDB](_.write(point))
  def close(): URIO[InfluxDB, Unit] =
    serviceWithZIO[InfluxDB](_.close())
}

case class InfluxDBLive(client: InfluxDBClient) extends InfluxDB {
  private val writeApi = client.getWriteApiBlocking

  override def write(point: Point): Task[Unit] =
    attempt(writeApi.writePoint(point)).orDie

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
      )(influx => influx.close())
    }

  val readConfig: ZIO[Any, String, InfluxDBLive.InfluxConfig] =
    for
      url <- ZIO
        .attempt(sys.env.getOrElse("INFLUXDB_URL", "http://0.0.0.0:8086"))
        .map(new URL(_))
        .catchAll(_ => ZIO.fail("Wot?"))
      token  <- ZIO.fromOption(sys.env.get("INFLUXDB_ADMIN_USER_TOKEN")).catchAll(_ => ZIO.fail("Missing \"token\""))
      org    <- ZIO.fromOption(sys.env.get("INFLUXDB_ORG")).catchAll(_ => ZIO.fail("Missing \"org\""))
      bucket <- ZIO.fromOption(sys.env.get("INFLUXDB_BUCKET")).catchAll(_ => ZIO.fail("Missing \"bucket\""))
    yield (url, token, org, bucket)
}
