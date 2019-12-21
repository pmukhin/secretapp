package com.newmotion.tvi.client

import java.time.{LocalDate, LocalDateTime}

import cats.data.Kleisli
import cats.effect.Sync
import io.circe.{Encoder, Json}

case class Client(
  id: Long,
  firstName: String,
  lastName: String,
  bod: LocalDate,
  isDeleted: Boolean
)

object Client {
  import io.circe.generic.semiauto._
  import cats.syntax.applicative._
  import cats.syntax.apply._
  import io.scalaland.chimney.dsl._

  implicit val dateEncoder: Encoder[LocalDate] =
    (a: LocalDate) => Json.fromString(a.formatted("YYYY-mm-dd"))

  implicit val encoder: Encoder[Client] =
    deriveEncoder

  case class CreateCommand(
    firstName: String,
    lastName: String,
    bod: LocalDate
  )

  case object DriverTooYoung extends RuntimeException
  case object AlreadyDeleted extends RuntimeException

  // noinspection TypeAnnotation
  def createK[F[_]: Sync](clients: ClientRepository[F]) =
    validateK.andThen(writeK(clients))

  // noinspection TypeAnnotation
  def deleteK[F[_]: Sync](clients: ClientRepository[F]) =
    Kleisli[F, Long, Unit](clients.drop)

  // noinspection TypeAnnotation
  def validateK[F[_]](implicit F: Sync[F]) =
    Kleisli[F, CreateCommand, CreateCommand] { cmd =>
      val bod           = cmd.bod.atStartOfDay
      val youngerThan18 = LocalDateTime.now().minusYears(18L).isBefore(bod)

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
