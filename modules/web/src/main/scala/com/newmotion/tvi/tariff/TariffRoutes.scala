package com.newmotion.tvi.tariff

import cats.data.Kleisli
import cats.effect.Sync
import com.newmotion.logging.FLogger
import com.newmotion.tvi.Repository
import com.newmotion.tvi.decoders.{OptDeletedParam, OptLimitParam, OptOffsetParam}
import com.newmotion.response.BadRequestBody
import com.newmotion.tvi.Repository.AlreadyDeleted
import org.http4s._
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl

object TariffRoutes {

  implicit def tariffEnc[F[_]](implicit F: Sync[F]): EntityEncoder[F, Tariff] =
    jsonEncoderOf[F, Tariff]

  implicit def tariffListEnc[F[_]](
    implicit F: Sync[F]
  ): EntityEncoder[F, List[Tariff]] =
    jsonEncoderOf[F, List[Tariff]]

  implicit def tariffOptEnc[F[_]](
    implicit F: Sync[F]
  ): EntityEncoder[F, Option[Tariff]] =
    jsonEncoderOf[F, Option[Tariff]]

  import TariffRequest._

  implicit def tariffReqDec[F[_]](
    implicit F: Sync[F]
  ): EntityDecoder[F, TariffRequest] =
    jsonOf[F, TariffRequest]

  // simple factory for our fellow tariff controller
  def impl[F[_]: Sync](
    tariffs: TariffRepository[F]
  ): TariffRoutes[F] =
    new TariffRoutes(
      tariffs,
      Tariff.createK[F](tariffs),
      Tariff.deleteK[F](tariffs),
      FLogger.fromClassUnsafe[F, TariffRoutes[F]] // let it fail immediately if something's wrong
    )
}

class TariffRoutes[F[_]](
  tariffs: TariffRepository[F],
  createK: Kleisli[F, Tariff.CreateCommand, Tariff],
  deleteK: Kleisli[F, Long, Unit],
  logger: FLogger[F]
)(implicit F: Sync[F])
    extends Http4sDsl[F] {
  // Tariff-related actions
  import TariffRoutes._
  import cats.syntax.all._
  import com.newmotion.tvi.helpers._
  import io.scalaland.chimney.dsl._

  private val defLimit  = 30
  private val defOffset = 0

  val routes: HttpRoutes[F] = HttpRoutes.of[F] {
    case GET -> Root :? OptOffsetParam(off) +& OptLimitParam(lim) +& OptDeletedParam(del) =>
      val criteria = Repository
        .Criteria(
          lim.map(_.toInt).getOrElse(defLimit),
          off.map(_.toInt).getOrElse(defOffset),
          del.exists(_.toBool)
        )
      tariffs.findAll(criteria).andOk

    case GET -> Root / LongVar(id) =>
      tariffs.findById(id).orNotFound

    case req @ POST -> Root =>
      val ops =
        req.as[TariffRequest].map(_.transformInto[Tariff.CreateCommand]) >>=
          createK.run

      ops.andOk.recoverWith {
        case Tariff.StartsInThePast =>
          BadRequest(BadRequestBody("starts in the past" :: Nil))
        case Tariff.ConflictingTariffExists =>
          BadRequest(BadRequestBody("conflicting tariff exists" :: Nil))
        case e: InvalidMessageBodyFailure =>
          logger.info(e)("body parsing failure") *> F.raiseError(e)
      }

    case DELETE -> Root / LongVar(id) =>
      tariffs
        .findById(id)
        .orNotFoundAndThen_(deleteK.run(id))
        .handleErrorWith {
          case AlreadyDeleted =>
            BadRequest(BadRequestBody("entity already deleted" :: Nil))
        }
  }
}
