package com.newmotion.it

import java.util.concurrent.Executors

import cats.effect.{Blocker, IO}
import com.newmotion.it.helpers.{Configuration, HttpRequests}
import doobie.util.transactor.Transactor
import org.specs2.mutable.Specification
import org.specs2.specification.AfterAll

import scala.concurrent.ExecutionContext
import scala.util.Try

trait IntegrationSpec
    extends Specification
    with Configuration
    with HttpRequests
    with AfterAll
    with Queries { self =>

  override protected val className = self.getClass.getSimpleName

  protected val transactor: Transactor.Aux[IO, Unit] =
    Transactor.fromDriverManager[IO](
      driver = "com.mysql.jdbc.Driver",
      url = conf.database.jdbcUrl,
      user = conf.database.username,
      pass = conf.database.password,
      blocker = Blocker.liftExecutionContext(
        ExecutionContext.fromExecutor(Executors.newFixedThreadPool(2))
      )
    )

  import cats.implicits._
  import doobie._
  import doobie.implicits._

  protected def clean(): Unit =
    Try {
      allTables
        .transact(transactor)
        .flatMap { tables =>
          tables
            .map(truncate(_).transact(transactor))
            .sequence_
        }
        .unsafeRunSync
    }.toEither.leftMap(e => failure(e.getMessage))

  override def afterAll(): Unit = clean()

  protected def seed(name: String): Unit = {
    clean()
    val sql = loadResource(s"/testdata/$className/sql/$name.sql")
    Try(Fragment.const(sql).update.run.transact(transactor).unsafeRunSync).toEither.leftMap(println)
  }

  protected def asIn(name: String): String =
    loadResource(s"/testdata/$className/request_$name.json")
}
