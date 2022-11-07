package com.pinkstack.loglog

import com.influxdb.client.domain.WritePrecision
import com.influxdb.client.write.Point
import zio.{Console, Queue, RIO, ZIO}
import ZIO.{attempt, service, succeed}
import Console.printLine

object Pusher:
  private val measurementToPoint: ChannelMeasurement => Point = measurement =>
    Point
      .measurement("concurrent_viewers")
      .addTag("channel", measurement.channel.name)
      .addTag("media", if (measurement.channel.name.contains("tv")) "tv" else "ra")
      .addField("count", measurement.count)
      .time(measurement.createdAt, WritePrecision.MS)

  def observePushAndLog(measurements: Queue[ChannelMeasurement]): ZIO[InfluxDB, Throwable, Unit] =
    measurements.take
      .map(measurement => (measurement, measurementToPoint(measurement)))
      .flatMap { case (measurement, point) =>
        InfluxDB.write(point)
          *> printLine(
            s"Current viewers collected. Channel: ${measurement.channel.name}, " +
              s"Count: ${measurement.count}, " +
              s"Source: ${measurement.channel.url}"
          ) *> succeed(println(point.toLineProtocol))
      }
      .forever
