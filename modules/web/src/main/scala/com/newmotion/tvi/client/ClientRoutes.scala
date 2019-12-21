package com.newmotion.tvi.client

import java.time.{Instant, ZonedDateTime}

import cats.data.Kleisli
import cats.effect.Sync
import com.newmotion.logging.FLogger
import com.newmotion.tvi.{Repository, formats}
import com.newmotion.tvi.client.Client.AlreadyDeleted
import com.newmotion.response.BadRequestBody
import com.newmotion.tvi.client.invoice.Invoice
import org.http4s.circe.{jsonEncoderOf, jsonOf}
import org.http4s.dsl.Http4sDsl
import org.http4s.{EntityDecoder, EntityEncoder, HttpRoutes}

import scala.util.Try

object ClientRoutes {
  import ClientRequest._

  implicit def clientReqDec[F[_]](
    implicit F: Sync[F]
  ): EntityDecoder[F, ClientRequest] =
    jsonOf[F, ClientRequest]

  implicit def clientEnc[F[_]](
    implicit F: Sync[F]
  ): EntityEncoder[F, Client] =
    jsonEncoderOf[F, Client]

  implicit def clientListEnc[F[_]](
    implicit F: Sync[F]
  ): EntityEncoder[F, List[Client]] =
    jsonEncoderOf[F, List[Client]]

  def impl[F[_]: Sync](clients: ClientRepository[F]): ClientRoutes[F] =
    new ClientRoutes[F](
      clients,
      Client.createK(clients),
      Client.deleteK(clients),
      FLogger.fromClassUnsafe[F, ClientRoutes[F]]
    )
}

class ClientRoutes[F[_]](
  clients: ClientRepository[F],
  createK: Kleisli[F, Client.CreateCommand, Client],
  deleteK: Kleisli[F, Long, Unit],
  logger: FLogger[F]
)(implicit F: Sync[F])
    extends Http4sDsl[F] {
  // client related endpoints
  import ClientRoutes._
  import cats.implicits._
  import com.newmotion.tvi.decoders._
  import com.newmotion.tvi.helpers._
  import io.scalaland.chimney.dsl._

  private val defLimit  = 20
  private val defOffset = 0

  val routes: HttpRoutes[F] = HttpRoutes.of[F] {
    case GET -> Root :? OptOffsetParam(off) +& OptLimitParam(lim) +& OptDeletedParam(del) =>
      val criteria = Repository
        .Criteria(
          lim.map(_.toInt).getOrElse(defLimit),
          off.map(_.toInt).getOrElse(defOffset),
          del.exists(_.toBool)
        )
      clients.findAll(criteria).andOk

    case GET -> Root / LongVar(id) =>
      clients.findById(id).orNotFound

    case DELETE -> Root / LongVar(id) =>
      clients
        .findById(id)
        .orNotFoundAndThen_(deleteK.run(id))
        .handleErrorWith {
          case AlreadyDeleted =>
            BadRequest(BadRequestBody("entity already deleted" :: Nil))
        }

    case req @ POST -> Root =>
      val create = req
        .as[ClientRequest]
        .map(_.transformInto[Client.CreateCommand]) >>= createK.run

      create.andOk.recoverWith {
        case Client.DriverTooYoung =>
          BadRequest(BadRequestBody("driver is too young" :: Nil))
      }
  }
}
