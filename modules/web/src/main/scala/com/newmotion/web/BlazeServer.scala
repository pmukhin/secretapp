package com.newmotion.web

import cats.effect.{ConcurrentEffect, ExitCode, Timer}
import org.http4s.HttpApp
import org.http4s.server.blaze.BlazeServerBuilder

object BlazeServer {

  def fromConf[F[_]: ConcurrentEffect: Timer](app: HttpApp[F], port: Int): fs2.Stream[F, ExitCode] =
    BlazeServerBuilder[F]
      .bindHttp(port, "0.0.0.0")
      .withHttpApp(app)
      .serve
}
