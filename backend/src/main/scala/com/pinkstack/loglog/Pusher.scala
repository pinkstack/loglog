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

  def observeAndPush(measurements: Queue[ChannelMeasurement]): ZIO[CoraLogger, Throwable, Unit] = {
    def writer =
      for
        logger      <- service[CoraLogger]
        measurement <- measurements.take
        point       <- succeed(measurementToPoint(measurement))
        _           <- succeed(println(point.toLineProtocol))
        _           <- attempt {
          logger.counter("collection", "viewer-cnt-test", measurement.count)
        }
        _           <- attempt(InfluxDB.write(point))
      yield ()

    writer.forever
  }
  // measurements.take
  //   .tap(p => )
  //   .map(measurementToPoint)
  //   .tap(p => ZIO.succeed(println(p.toLineProtocol)))
  //   .flatMap(InfluxDB.write)
  //   .forever
