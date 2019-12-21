package com.newmotion.tvi.session

import java.time.ZonedDateTime

import cats.data.Kleisli
import cats.effect.Sync
import eu.timepit.refined.api.Refined
import eu.timepit.refined.numeric.Positive
import io.circe.Decoder
import io.circe.generic.semiauto._

case object SessionRequest {
  import io.circe.refined._ // don't delete even if Intellij wants you to

  implicit val decoder: Decoder[SessionRequest] =
    deriveDecoder

  import cats.implicits._
  import io.scalaland.chimney.dsl._

  case object StartIsAfterEnd extends Throwable
  case object EmptyInterval   extends Throwable

  // noinspection TypeAnnotation
  def toCreateCommandK[F[_]](implicit F: Sync[F]) =
    Kleisli[F, SessionRequest, Session.CreateCommand] { req =>
      F.raiseError(StartIsAfterEnd).whenA(req.start.isAfter(req.end)) *>
        F.raiseError(EmptyInterval).whenA(req.start.isEqual(req.end)) *>
        F.pure(
          req
            .into[Session.CreateCommand]
            .withFieldConst(_.clientId, req.clientId.value)
            .withFieldConst(_.chargePointId, req.chargePointId.value)
            .withFieldConst(_.start, req.start.toInstant)
            .withFieldConst(_.end, req.end.toInstant)
            .transform
        )
    }
}

case class SessionRequest(
  clientId: Refined[Long, Positive],
  chargePointId: Refined[Long, Positive],
  start: ZonedDateTime,
  end: ZonedDateTime
) extends Product
