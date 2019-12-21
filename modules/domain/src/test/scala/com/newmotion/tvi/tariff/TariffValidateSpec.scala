package com.newmotion.tvi.tariff

import java.time.{Instant, ZoneId, ZonedDateTime}

import cats.effect.IO
import cats.effect.specs2.CatsIO
import org.specs2.matcher._
import org.specs2.mutable.Specification

class TariffValidateSpec
    extends Specification
    with CatsIO
    with org.mockito.specs2.Mockito
    with Matchers {
  private val timeZone = ZoneId.of("GMT")

  private val timeInPast =
    ZonedDateTime.of(2018, 1, 1, 0, 0, 0, 0, timeZone)
  private val timeInFuture  = timeInPast.withYear(2036)
  private val sampleCommand = Tariff.CreateCommand(2.0d, Some(2.0d), timeInPast.toInstant)

  private val sampleTariff =
    Tariff(42, 2.2d, None, start = timeInFuture.toInstant, false)

  import org.mockito.Mockito._

  "Tariff.validateK" should {
    "raiseError if invalid tariff starts in past" in {
      val repo = mock[TariffRepository[IO]]
      when(repo.findByStart(*[Instant])).thenReturn(IO.pure(Option.empty[Tariff]))
      Tariff.validateK[IO](repo).run(sampleCommand).attempt.map(_.isLeft must beTrue)
    }

    "raiseError if there's a conflicting tariff" in {
      val repo = mock[TariffRepository[IO]]
      when(repo.findByStart(*[Instant])).thenReturn(IO.pure(Some(sampleTariff)))
      Tariff
        .validateK[IO](repo)
        .run(sampleCommand.copy(start = timeInFuture.toInstant))
        .attempt
        .map(_.isLeft must beTrue)
    }

    "do nothing if tariff is valid" in {
      val repo = mock[TariffRepository[IO]]
      when(repo.findByStart(*[Instant])).thenReturn(IO.pure(Option.empty[Tariff]))
      Tariff
        .validateK[IO](repo)
        .run(sampleCommand.copy(start = timeInFuture.toInstant))
        .attempt
        .map(_.isRight must beTrue)
    }
  }
}
