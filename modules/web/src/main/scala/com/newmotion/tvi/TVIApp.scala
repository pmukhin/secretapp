package com.newmotion.tvi

import cats.effect.{ConcurrentEffect, ContextShift, Timer}
import com.newmotion.configuration.{DatabaseConf, WebConf}
import com.newmotion.database.DoobieTransactor
import com.newmotion.tvi.client.invoice.InvoiceRoutes
import com.newmotion.tvi.client.{ClientRoutes, DoobieClientRepo}
import com.newmotion.tvi.session.{CreateSessionConsumer, DoobieSessionRepo, Session, SessionRoutes}
import com.newmotion.tvi.tariff.{DoobieTariffRepo, TariffRoutes}
import com.newmotion.web.BlazeServer
import fs2.Stream
import fs2.concurrent.Queue
import org.http4s.implicits._
import org.http4s.server.Router
import org.http4s.server.middleware.Logger
import pureconfig.ConfigSource

object TVIApp {
  import com.newmotion.tvi.implicits._
  import pureconfig.generic.auto._
  import pureconfig.module.catseffect._

  def stream[F[_]](
    implicit
    F: ConcurrentEffect[F],
    T: Timer[F],
    C: ContextShift[F]
  ): Stream[F, Nothing] = {
    for {
      dbConf          <- ConfigSource.default.at(DatabaseConf.section).loadF[F, DatabaseConf].stream
      webConf         <- ConfigSource.default.at(WebConf.section).loadF[F, WebConf].stream
      xa              = DoobieTransactor.fromConf(dbConf)
      tariffs         = DoobieTariffRepo.impl[F](xa)
      clients         = DoobieClientRepo.impl[F](xa)
      sessions        = DoobieSessionRepo.impl[F](xa)
      newSessionQueue <- Queue.bounded[F, Session.CreateCommand](2048).stream
      sessionEvSink   = LocalEventSink.impl[F, Session.CreateCommand](newSessionQueue)
      sessionCons = CreateSessionConsumer
        .fromK(Session.createK_(sessions), newSessionQueue, 8)

      router = Router(
        "/tariffs"  -> TariffRoutes.impl[F](tariffs).routes,
        "/clients"  -> ClientRoutes.impl[F](clients).routes,
        "/sessions" -> SessionRoutes.impl[F](sessions, sessionEvSink).routes,
        "/invoices" -> InvoiceRoutes.impl[F](sessions, tariffs, clients).routes
      ).orNotFound

      app      = Logger.httpApp(logHeaders = true, logBody = false)(router)
      exitCode <- BlazeServer.fromConf(app, webConf.port).concurrently(sessionCons.start)
    } yield exitCode
  }.drain
}
