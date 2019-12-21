package com.newmotion.tvi.client.invoice

import java.time.{Instant, LocalDateTime, ZoneOffset, ZonedDateTime}
import java.util.concurrent.TimeUnit

import cats.effect.IO
import cats.effect.specs2.CatsIO
import com.newmotion.tvi.session.{Session, SessionRepository}
import com.newmotion.tvi.tariff.{Tariff, TariffRepository}
import org.mockito.specs2.Mockito
import org.specs2.mutable.Specification

class InvoiceBuildKSpec extends Specification with Mockito with CatsIO {
  import java.time.format.DateTimeFormatter

  import org.mockito.Mockito._

  private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

  private def makeInstant(s: String): Instant =
    Instant.from(ZonedDateTime.of(LocalDateTime.parse(s, formatter), ZoneOffset.UTC))

  private val secondsInADay = TimeUnit.DAYS.toSeconds(1)

  private val instant = makeInstant("2018-04-13 12:15:34")

  private val session =
    Session(1, 24, 171, instant, instant.plusSeconds(60 * 60))

  private val tariff =
    Tariff(1, 2.2d, Some(3.3d), instant.minusSeconds(secondsInADay * 2), isDeleted = false)

  "Invoice.buildK" should {
    "build an empty Invoice when client hasn't had sessions" in {
      val sessions = mock[SessionRepository[IO]]
      when(sessions.findByClient(*[Long], *[Instant], *[Instant]))
        .thenReturn(IO.pure(List()))
      val tariffs = mock[TariffRepository[IO]]
      Invoice
        .buildK[IO](sessions, tariffs)
        .run(Invoice.GenerateCommand(24, instant, instant))
        .map {
          case Invoice(_, List()) => ok
          case _                  => ko(s"unexpected result")
        }
    }

    "build an Invoice when data is provided" in {
      val sessions = mock[SessionRepository[IO]]
      when(sessions.findByClient(*[Long], *[Instant], *[Instant]))
        .thenReturn(
          IO.pure(
            List(
              session,
              session.copy(
                start = session.start.plusSeconds(secondsInADay),
                end = session.end.plusSeconds(secondsInADay)
              ),
              session.copy(
                start = session.start.plusSeconds(secondsInADay * 5),
                end = session.end.plusSeconds(secondsInADay * 5)
              )
            )
          )
        )

      val tariffs = mock[TariffRepository[IO]]
      when(tariffs.findWithin(*[Instant], *[Instant]))
        .thenReturn(
          IO.pure(
            List(
              tariff,
              tariff.copy(
                hourlyRate = 12.2,
                hourlyParkingRate = None,
                start = tariff.start.plusSeconds(secondsInADay * 6)
              )
            )
          )
        )

      Invoice
        .buildK[IO](sessions, tariffs)
        .run(Invoice.GenerateCommand(24, instant, instant))
        .map { invoice =>
          invoice.sessions.map(s => (s.totalEnergy, s.totalParking)) match {
            case List((2.2, 3.3), (2.2, 3.3), (12.2, 0.0)) => ok
          }
        }
    }
  }
}
