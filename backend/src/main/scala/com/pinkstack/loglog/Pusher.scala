package com.pinkstack.loglog

import com.influxdb.client.domain.WritePrecision
import com.influxdb.client.write.Point
import zio.{Queue, RIO}

object Pusher:
  val measurementToPoint: ChannelMeasurement => Point = measurement =>
    Point
      .measurement("concurrent_viewers")
      .addTag("channel", measurement.channel.name)
      .addTag("media", if (measurement.channel.name.contains("tv")) "tv" else "ra")
      .addField("count", measurement.count)
      .time(measurement.createdAt, WritePrecision.MS)

  val observeAndPush: Queue[ChannelMeasurement] => RIO[InfluxDB, Unit] =
    _.take
      .map(measurementToPoint)
      .flatMap(InfluxDB.write)
      .forever
