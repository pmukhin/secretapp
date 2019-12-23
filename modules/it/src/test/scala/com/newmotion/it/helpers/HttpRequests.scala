package com.newmotion.it.helpers

import java.util.concurrent.Executors

import cats.effect.{ContextShift, IO, Resource}
import fs2.Chunk
import io.circe.JsonObject
import io.circe.parser.decode
import org.http4s.client.Client
import org.http4s.client.blaze.BlazeClientBuilder
import org.http4s.{Method, Request, Uri}
import org.specs2.matcher.MatchResult
import org.specs2.mutable.Specification

import scala.concurrent.ExecutionContext

trait HttpRequests { self: Configuration with Specification =>

  implicit val ec: ExecutionContext = ExecutionContext
    .fromExecutor(Executors.newCachedThreadPool)
  implicit val cs: ContextShift[IO] = IO.contextShift(ec)

  protected val className: String

  private val client: Resource[IO, Client[IO]] = BlazeClientBuilder[IO](ec).resource

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
      val uri = Uri.unsafeFromString(conf.baseUri + path)
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
      val uri = Uri.unsafeFromString(conf.baseUri + path)
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
