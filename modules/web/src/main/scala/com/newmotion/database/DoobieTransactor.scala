package com.newmotion.database

import java.util.concurrent.Executors

import cats.effect.{Async, Blocker, ContextShift}
import com.newmotion.configuration.DatabaseConf
import doobie.util.transactor.Transactor

import scala.concurrent.ExecutionContext

object DoobieTransactor {

  private val mysqlDriver =
    "com.mysql.jdbc.Driver"

  def fromConf[F[_]: Async: ContextShift](conf: DatabaseConf): Transactor.Aux[F, Unit] =
    Transactor.fromDriverManager[F](
      driver = mysqlDriver,
      url = conf.url,
      user = conf.username,
      pass = conf.password,
      blocker = Blocker.liftExecutionContext(
        ExecutionContext.fromExecutor(Executors.newFixedThreadPool(conf.threads))
      )
    )
}
