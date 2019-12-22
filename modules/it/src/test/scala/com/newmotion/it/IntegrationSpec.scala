package com.newmotion.it

import java.util.concurrent.Executors

import cats.effect.{Blocker, IO}
import doobie.util.transactor.Transactor
import fs2.Chunk
import io.circe.JsonObject
import org.http4s.client.blaze.BlazeClientBuilder
import org.http4s.{Method, Request, Uri}
import org.specs2.matcher.MatchResult
import org.specs2.mutable.Specification
import pureconfig.ConfigSource

import scala.concurrent.ExecutionContext
import scala.io.Source
import scala.util.Try

trait IntegrationSpec { self: Specification =>

  import pureconfig.generic.auto._

  implicit val ec = ExecutionContext
    .fromExecutor(Executors.newCachedThreadPool)

  implicit val cs = IO.contextShift(ec)

  private case class DatabaseConf(
    host: String,
    port: Int,
    name: String,
    username: String,
    password: String,
    threads: Int
  ) extends Product { lazy val url = s"jdbc:mysql://$host:$port/$name" }

  private val baseUri = "http://127.0.0.1:8080"
  private val conf    = ConfigSource.default.load[DatabaseConf].fold(throw _, identity)

  protected val transactor: Transactor.Aux[IO, Unit] =
    Transactor.fromDriverManager[IO](
      driver = "com.mysql.jdbc.Driver",
      url = conf.url,
      user = conf.username,
      pass = conf.password,
      blocker = Blocker.liftExecutionContext(
        ExecutionContext.fromExecutor(Executors.newFixedThreadPool(conf.threads))
      )
    )

  private val className = self.getClass.getSimpleName
  private val client    = BlazeClientBuilder[IO](ec).resource

  private def loadResource(n: String) = {
    val resource = getClass.getResource(n)
    Try(Source.fromURL(resource).getLines.mkString("\n")).toOption
      .getOrElse(throw new RuntimeException(s"resource: $n can not be opened"))
  }

  protected def asIn(name: String): String =
    loadResource(s"/testdata/$className/request_$name.json")

  import io.circe.parser._

  case class Result(status: Int, body: String) {

    def bodyAsIn(
      name: String,
      asJson: Boolean = false,
      dropColumns: List[String] = List()
    ): MatchResult[Any] = {
      val data = loadResource(s"/testdata/$className/response_$name.json")
      if (!asJson) {
        data must_== body
      } else {
        val expected = decode[JsonObject](data).fold(throw _, identity)
        val actual = decode[JsonObject](body).fold(throw _, identity).filter {
          case (key, _) => !dropColumns.contains(key)
        }
        if (expected.equals(actual)) ok else ko(s"$expected != $actual")
      }
    }
  }

  protected def get(path: String): IO[Result] =
    client.use { c =>
      val uri = Uri.unsafeFromString(baseUri + path)
      c.get[Result](uri) { r =>
        r.body.compile.toList
          .map { bytes =>
            new String(bytes.toArray)
          }
          .map(Result(r.status.code, _))
      }
    }

  protected def post(path: String, body: String = ""): IO[Result] =
    fetch(Method.POST, path, body)

  protected def patch(path: String, body: String = ""): IO[Result] =
    fetch(Method.PATCH, path, body)

  protected def put(path: String, body: String = ""): IO[Result] =
    fetch(Method.PUT, path, body)

  protected def delete(path: String, body: String = ""): IO[Result] =
    fetch(Method.DELETE, path, body)

  private def fetch(m: Method, path: String, body: String): IO[Result] =
    client.use { c =>
      val uri = Uri.unsafeFromString(baseUri + path)
      val req = Request[IO](m, uri, body = fs2.Stream.chunk(Chunk.bytes(body.getBytes)))
      c.fetch[Result](req) { r =>
        r.body.compile.toList
          .map { bytes =>
            new String(bytes.toArray)
          }
          .map(Result(r.status.code, _))
      }
    }
}
