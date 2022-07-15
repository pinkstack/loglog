package com.pinkstack.loglog

import java.net.URL
import zio.json.*
import zio.json.yaml.*

import java.time.Instant

type Channels = Vector[Channel]

given Conversion[URL, String] with
  override def apply(url: URL): String = url.toString

given Conversion[String, URL] with
  override def apply(s: String): URL = new URL(s)

case class Channel(name: String, url: URL, enabled: Boolean)

object Channel {
  given urlDecoder: JsonDecoder[URL]         = JsonDecoder[String].map(new URL(_))
  given channelDecoder: JsonDecoder[Channel] = DeriveJsonDecoder.gen[Channel]
}

case class ChannelMeasurement(channel: Channel, count: Int = 0, createdAt: Long = Instant.now().toEpochMilli)
