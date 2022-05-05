package com.pinkstack.loglog

import zio.*
import zio.ZIO.{acquireRelease, acquireReleaseWith, attempt, attemptBlocking}

object Resources {
  private def resourceFrom(resourceName: String) =
    attemptBlocking(scala.io.Source.fromURL(ClassLoader.getSystemResource(resourceName)))

  def read(resourceName: String): Task[String] =
    acquireReleaseWith(resourceFrom(resourceName))(source => attempt(source.close()).orDie) { source =>
      attemptBlocking(source.getLines().mkString("\n"))
    }

  def scopedFrom(resourceName: String) =
    ZIO.scoped {
      acquireRelease(resourceFrom(resourceName))(source => attempt(source.close()).orDie)
        .flatMap(source => attemptBlocking(source.getLines().mkString("\n")))
    }
}
