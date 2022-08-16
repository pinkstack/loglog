package com.pinkstack.loglog

import com.influxdb.client.domain.WritePrecision
import com.influxdb.client.write.Point
import zio.{Queue, RIO, ZIO}
import ZIO.{attempt, service, succeed}

object Pusher:
  val measurementToPoint: ChannelMeasurement => Point = measurement =>
    Point
      .measurement("concurrent_viewers")
      .addTag("channel", measurement.channel.name)
      .addTag("media", if (measurement.channel.name.contains("tv")) "tv" else "ra")
      .addField("count", measurement.count)
      .time(measurement.createdAt, WritePrecision.MS)

  def observePushAndLog(measurements: Queue[ChannelMeasurement]): ZIO[CoraLogger & InfluxDB, Throwable, Unit] =
    service[CoraLogger].flatMap { logger =>
      measurements.take
        .map(measurement => (measurement, measurementToPoint(measurement)))
        .flatMap { case (measurement, point) =>
          InfluxDB.write(point)
            *> logger.info(
              s"Current viewers collected. Channel: ${measurement.channel.name}, " +
                s"Count: ${measurement.count}, " +
                s"Source: ${measurement.channel.url}"
            ) *> succeed(println(point.toLineProtocol))
        }
        .forever
    }
