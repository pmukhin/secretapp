package com.newmotion.tvi.client.invoice

import java.time.LocalDate

import cats.effect.IO
import cats.effect.specs2.CatsIO
import com.newmotion.tvi.client.{Client, ClientRepository}
import com.newmotion.tvi.session.SessionRepository
import com.newmotion.tvi.tariff.TariffRepository
import org.http4s.{Method, Request, Status, Uri}
import org.mockito.specs2.Mockito
import org.specs2.mutable.Specification

class InvoiceRoutesSpec extends Specification with Mockito with CatsIO {
  import org.http4s.implicits._
  import org.mockito.Mockito._

  private val client =
    Client(25, "Test1", "Test2", LocalDate.of(1989, 8, 10), isDeleted = false)

  "InvoiceRoutes" should {
    "return BadRequest when start is after end" in {
      val clients = mock[ClientRepository[IO]]
      when(clients.findById(25)).thenReturn(IO.pure(Some(client)))
      val tariffs  = mock[TariffRepository[IO]]
      val sessions = mock[SessionRepository[IO]]
      val req = Request[IO](
        method = Method.GET,
        uri = Uri.uri("/25/2017-05-12T23:20:50Z/2017-04-12T23:20:50Z.csv")
      )
      InvoiceRoutes.impl[IO](sessions, tariffs, clients).routes.orNotFound.run(req).map { r =>
        r.status must_== Status.BadRequest
      }
    }
    "return BadRequest when start == end" in {
      val clients = mock[ClientRepository[IO]]
      when(clients.findById(25)).thenReturn(IO.pure(Some(client)))
      val tariffs  = mock[TariffRepository[IO]]
      val sessions = mock[SessionRepository[IO]]
      val req = Request[IO](
        method = Method.GET,
        uri = Uri.uri("/25/2017-04-12T23:20:50Z/2017-04-12T23:20:50Z.csv")
      )
      InvoiceRoutes.impl[IO](sessions, tariffs, clients).routes.orNotFound.run(req).map { r =>
        r.status must_== Status.BadRequest
      }
    }
    "return BadRequest when end is in future" in {
      val clients = mock[ClientRepository[IO]]
      when(clients.findById(25)).thenReturn(IO.pure(Some(client)))
      val tariffs  = mock[TariffRepository[IO]]
      val sessions = mock[SessionRepository[IO]]
      val req = Request[IO](
        method = Method.GET,
        uri = Uri.uri("/25/2017-04-12T23:20:50Z/2024-04-12T23:20:50Z.csv")
      )
      InvoiceRoutes.impl[IO](sessions, tariffs, clients).routes.orNotFound.run(req).map { r =>
        r.status must_== Status.BadRequest
      }
    }
    "return NotFound when client does not exist" in {
      val clients = mock[ClientRepository[IO]]
      when(clients.findById(25)).thenReturn(IO.pure(None))
      val tariffs  = mock[TariffRepository[IO]]
      val sessions = mock[SessionRepository[IO]]
      val req = Request[IO](
        method = Method.GET,
        uri = Uri.uri("/25/2017-04-12T23:20:50Z/2017-04-12T23:20:50Z.csv")
      )
      InvoiceRoutes.impl[IO](sessions, tariffs, clients).routes.orNotFound.run(req).map { r =>
        r.status must_== Status.NotFound
      }
    }
  }
}
