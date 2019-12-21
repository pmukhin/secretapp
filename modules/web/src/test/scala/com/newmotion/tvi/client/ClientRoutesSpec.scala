package com.newmotion.tvi.client

import java.time.LocalDate

import cats.effect.IO
import cats.effect.specs2.CatsIO
import org.http4s.{Method, Request, Status, Uri}
import org.mockito.specs2.Mockito
import org.specs2.mutable.Specification

class ClientRoutesSpec extends Specification with Mockito with CatsIO {
  import org.mockito.Mockito._
  import org.http4s.implicits._

  private val client =
    Client(25, "Test1", "Test2", LocalDate.of(1989, 8, 10), isDeleted = false)

  "ClientRoutes" should {
    "return an entity when it exists" in {
      val r = mock[ClientRepository[IO]]
      when(r.findById(25)).thenReturn(IO.pure(Some(client)))
      val request = Request[IO](Method.GET, Uri.uri("/25"))
      ClientRoutes.impl[IO](r).routes.orNotFound.run(request).flatMap { r =>
        r.status must_=== Status.Ok
        r.body.compile.toList.map { bl =>
          new String(bl.toArray) must_==
            """{"id":25,"firstName":"Test1","lastName":"Test2","dob":"1989-08-10"}"""
        }
      }
    }

    "return 404 when non-existent entity requested" in {
      val r = mock[ClientRepository[IO]]
      when(r.findById(25)).thenReturn(IO.pure(None))
      val request = Request[IO](Method.GET, Uri.uri("/25"))
      ClientRoutes.impl[IO](r).routes.orNotFound.run(request).map { r =>
        r.status must_=== Status.NotFound
      }
    }
  }
}
