package com.pinkstack.loglog

import java.net.URL
import zio.json.*
import zio.json.yaml.*

import java.time.Instant

type Channels = Vector[Channel]

case class Channel(name: String, url: URL, enabled: Boolean)

object Channel {
  implicit val urlDecoder: JsonDecoder[URL]         = JsonDecoder[String].map(new URL(_))
  implicit val channelDecoder: JsonDecoder[Channel] = DeriveJsonDecoder.gen[Channel]
}

case class ChannelMeasurement(channel: Channel, count: Int = 0, createdAt: Long = Instant.now().toEpochMilli)
