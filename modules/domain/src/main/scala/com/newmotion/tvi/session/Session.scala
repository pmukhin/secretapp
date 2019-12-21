package com.newmotion.tvi.session

import java.time.Instant

import cats.data.Kleisli
import cats.effect.Sync
import io.circe.Encoder

object Session {
  import io.circe.generic.semiauto._

  implicit val encoder: Encoder[Session] =
    deriveEncoder

  case class CreateCommand(
    clientId: Long,
    chargePointId: Long,
    start: Instant,
    end: Instant
  ) extends Product

  sealed trait ValidationErr extends RuntimeException
  case object TimeSpanBusy   extends ValidationErr

  // noinspection TypeAnnotation
  def createK[F[_]: Sync](sessions: SessionRepository[F]) =
    validateK(sessions).andThen(writeK(sessions))

  // noinspection TypeAnnotation
  def createK_[F[_]](sessions: SessionRepository[F])(implicit F: Sync[F]) =
    createK(sessions).andThen(Kleisli[F, Session, Unit](_ => F.unit))

  import cats.implicits._

  // noinspection TypeAnnotation
  def validateK[F[_]](sessions: SessionRepository[F])(implicit F: Sync[F]) =
    Kleisli[F, CreateCommand, CreateCommand] { cmd =>
      sessions.findByChargePoint(cmd.chargePointId, cmd.start, cmd.end) >>= {
        case Some(_) => F.raiseError[CreateCommand](TimeSpanBusy)
        case _       => F.pure(cmd)
      }
    }

  private def writeK[F[_]](sessions: SessionRepository[F])(implicit F: Sync[F]) =
    Kleisli[F, CreateCommand, Session] { cmd =>
      import io.scalaland.chimney.dsl._
      val session = cmd
        .into[Session]
        .withFieldConst(_.id, 0L)
        .transform
      sessions :+ session
    }
}

case class Session(
  id: Long,
  clientId: Long,
  chargePointId: Long,
  start: Instant,
  end: Instant
)
