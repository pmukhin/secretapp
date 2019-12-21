package com.newmotion.tvi.client.invoice

import java.time.Instant
import java.util.concurrent.TimeUnit

import cats.data.Kleisli
import cats.effect.Sync
import com.newmotion.tvi.session.{Session, SessionRepository}
import com.newmotion.tvi.tariff.{Tariff, TariffRepository}
import io.circe.Encoder

object Invoice {
  import io.circe.generic.semiauto.deriveEncoder

  implicit val chargedSessionEncoder: Encoder[ChargedSession] =
    deriveEncoder

  implicit val encoder: Encoder[Invoice] =
    deriveEncoder

  case class ChargedSession(
    session: Session,
    tariff: Tariff,
    totalEnergy: Double,
    totalParking: Double
  )

  case class GenerateCommand(
    clientId: Long,
    start: Instant,
    end: Instant
  )

  def empty(clientId: Long) =
    Invoice(clientId, Nil)

  case class NoTariffForPeriod(start: Instant, end: Instant) extends RuntimeException

  import cats.syntax.flatMap._
  import cats.syntax.functor._

  // noinspection TypeAnnotation
  def buildK[F[_]](
    sessions: SessionRepository[F],
    tariffs: TariffRepository[F]
  )(implicit F: Sync[F]) = Kleisli[F, GenerateCommand, Invoice] { cmd =>
    // iterating through tariffs until we find a non-fitting
    def findTariff(sessionStart: Instant, tariffs: List[Tariff]): Option[Tariff] =
      tariffs match {
        case t :: xs =>
          if (t.start.isBefore(sessionStart) || t.start.equals(sessionStart))
            findTariff(sessionStart, xs).orElse(Some(t))
          else None
        case Nil => None
      }

    // where all the things happen
    def buildF(sessions: List[Session], tariffs: List[Tariff]): F[List[ChargedSession]] =
      sessions match {
        case session :: xs =>
          val tariff = findTariff(session.start, tariffs)
            .getOrElse(throw NoTariffForPeriod(session.start, session.end))
          val sessionLength = session.end.getEpochSecond - session.start.getEpochSecond
          val secondsInHour = TimeUnit.HOURS.toSeconds(1)
          val totalEnergy   = sessionLength * tariff.hourlyRate / secondsInHour
          val totalParking = sessionLength * tariff.hourlyParkingRate
            .map(_ / secondsInHour)
            .getOrElse(0d)
          buildF(xs, tariffs)
            .map(ChargedSession(session, tariff, totalEnergy, totalParking) :: _)
        case Nil => Sync[F].pure(Nil)
      }

    // then we need to find all of the tariffs inside the given interval + the one earlier
    def allTariffs(sessions: List[Session]): F[Invoice] =
      tariffs.findWithin(sessions.head.start, sessions.last.end) >>= { tfs =>
        if (tfs.isEmpty) // we don't expect having no tariff for a period so it's an error
          F.raiseError(NoTariffForPeriod(sessions.head.start, sessions.last.end))
        else
          buildF(sessions, tfs).map(Invoice(cmd.clientId, _))
      }

    // first we need to find all the sessions of a user in a given interval of time
    sessions.findByClient(cmd.clientId, cmd.start, cmd.end) >>=
      (ss => if (ss.isEmpty) F.pure(Invoice.empty(cmd.clientId)) else allTariffs(ss))
  }
}

case class Invoice(
  clientId: Long,
  sessions: List[Invoice.ChargedSession]
)
