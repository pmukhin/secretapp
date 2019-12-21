package com.newmotion.tvi.tariff

import java.time.{Instant, ZonedDateTime}

import cats.data.Kleisli
import cats.effect.Sync
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto._

case class Tariff(
  id: Long,
  hourlyRate: Double,
  hourlyParkingRate: Option[Double],
  start: Instant,
  isDeleted: Boolean
)

object Tariff {
  import io.scalaland.chimney.dsl._
  import cats.implicits._

  implicit val encoder: Encoder[Tariff] = deriveEncoder
  implicit val decoder: Decoder[Tariff] = deriveDecoder

  case class CreateCommand(
    hourlyRate: Double,
    hourlyParkingRate: Option[Double],
    start: Instant
  )

  case class UpdateCommand(
    id: Long,
    hourlyRate: Double,
    hourlyParkingRate: Option[Double],
    start: Instant
  )

  case object StartsInThePast         extends RuntimeException
  case object ConflictingTariffExists extends RuntimeException

  // noinspection TypeAnnotation
  def validateK[F[_]](tariffs: TariffRepository[F])(implicit F: Sync[F]) =
    Kleisli[F, CreateCommand, CreateCommand] { cmd =>
      F.raiseError(StartsInThePast).whenA(Instant.now.isAfter(cmd.start)) *>
        tariffs.findByStart(cmd.start) >>=
        (_.fold(F.pure(cmd))(_ => F.raiseError(ConflictingTariffExists)))
    }

  // noinspection TypeAnnotation
  def updateK[F[_]: Sync](tariffs: TariffRepository[F]) =
    Kleisli[F, (UpdateCommand, Tariff), Unit] {
      case (cmd, tariff) =>
        val upd = cmd
          .into[Tariff]
          .withFieldConst(_.isDeleted, tariff.isDeleted)
          .transform
        tariffs.update(upd)
    }

  // noinspection TypeAnnotation
  def createK[F[_]: Sync](tariffs: TariffRepository[F]) =
    validateK(tariffs).andThen(writeK(tariffs))

  def deleteK[F[_]](tariffs: TariffRepository[F])(implicit F: Sync[F]): Kleisli[F, Long, Unit] =
    Kleisli(tariffs.drop)

  private def writeK[F[_]: Sync](tariffs: TariffRepository[F]) =
    Kleisli[F, CreateCommand, Tariff] { cmd =>
      val tariff = cmd
        .into[Tariff]
        .withFieldConst(_.id, 0L)
        .withFieldConst(_.isDeleted, false)
        .transform
      tariffs :+ tariff
    }
}
