package com.newmotion.tvi.client

import java.time.format.DateTimeFormatter
import java.time.{LocalDate, LocalDateTime}

import cats.data.Kleisli
import cats.effect.Sync
import io.circe.{Decoder, Encoder, Json}

case class Client(
  id: Long,
  firstName: String,
  lastName: String,
  dob: LocalDate,
  isDeleted: Boolean
)

object Client {
  import cats.syntax.applicative._
  import cats.syntax.apply._
  import io.scalaland.chimney.dsl._

  private val dobEncoder =
    DateTimeFormatter.ofPattern("yyyy-MM-dd")

  implicit val dateEncoder: Encoder[LocalDate] =
    (a: LocalDate) => Json.fromString(dobEncoder.format(a))

  implicit val encoder: Encoder[Client] =
    Encoder.forProduct4("id", "firstName", "lastName", "dob") { c =>
      (c.id, c.firstName, c.lastName, c.dob)
    }

  case class CreateCommand(
    firstName: String,
    lastName: String,
    dob: LocalDate
  )

  case object DriverTooYoung extends RuntimeException

  // noinspection TypeAnnotation
  def createK[F[_]: Sync](clients: ClientRepository[F]) =
    validateK.andThen(writeK(clients))

  // noinspection TypeAnnotation
  def deleteK[F[_]: Sync](clients: ClientRepository[F]) =
    Kleisli[F, Long, Unit](clients.drop)

  // noinspection TypeAnnotation
  def validateK[F[_]](implicit F: Sync[F]) =
    Kleisli[F, CreateCommand, CreateCommand] { cmd =>
      val youngerThan18 = LocalDateTime
        .now()
        .minusYears(18L)
        .isBefore(cmd.dob.atStartOfDay)
      F.raiseError(DriverTooYoung).whenA(youngerThan18) *> F.pure(cmd)
    }

  private def writeK[F[_]: Sync](clients: ClientRepository[F]) =
    Kleisli[F, CreateCommand, Client] { cmd =>
      val client = cmd
        .into[Client]
        .withFieldConst(_.id, 0L)
        .withFieldConst(_.isDeleted, false)
        .transform
      clients :+ client
    }
}
