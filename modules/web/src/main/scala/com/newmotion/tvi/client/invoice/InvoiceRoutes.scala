package com.newmotion.tvi.client.invoice

import java.time.{Instant, ZonedDateTime}

import cats.data.{Kleisli, NonEmptyList, Validated, ValidatedNel}
import cats.effect.Sync
import com.newmotion.response.BadRequestBody
import com.newmotion.tvi.client.{Client, ClientRepository}
import com.newmotion.tvi.formats
import com.newmotion.tvi.session.SessionRepository
import com.newmotion.tvi.tariff.TariffRepository
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl
//import scala.language.implicitConversions

import scala.util.Try

object InvoiceRoutes {

  def impl[F[_]: Sync](
    sessions: SessionRepository[F],
    tariffs: TariffRepository[F],
    clients: ClientRepository[F]
  ): InvoiceRoutes[F] =
    new InvoiceRoutes(
      clients,
      Invoice.buildK(sessions, tariffs)
    )
}

class InvoiceRoutes[F[_]: Sync](
  clients: ClientRepository[F],
  buildK: Kleisli[F, Invoice.GenerateCommand, Invoice]
) extends Http4sDsl[F] {
  // everything to encode an Invoice to CSV
  import com.newmotion.tvi.helpers._
  import com.newmotion.tvi.validation
  import responseEncoders._

  private def toInstant(s: String): ValidatedNel[String, Instant] =
    Validated
      .fromTry(Try(ZonedDateTime.parse(s, formats.zonedDateTimeFormat).toInstant))
      .leftMap(_ => "failure parsing start/end")
      .toValidatedNel

  import cats.implicits._

  private val asBadReq = (nl: NonEmptyList[String]) => BadRequest(BadRequestBody(nl.toList))

  val routes: HttpRoutes[F] = HttpRoutes.of {
    case GET -> Root / LongVar(clientId) / start / end ~ "csv" =>
      val runCmd =
        (start: Instant, end: Instant, c: Client) =>
          buildK
            .run(Invoice.GenerateCommand(clientId, start, end))
            .andOk(csvEntityEncoder(fileNameFromClient(c, start, end) + s".csv"))

      val run = (c: Client) =>
        (toInstant(start), toInstant(end)) // some validation applied
          .mapN((_, _))
          .andThen((validation.instant.startBeforeEnd _).tupled)
          .andThen { case (s, e) => validation.instant.endIsInPast(e).map((s, _)) }
          .fold(asBadReq, { case (s, e) => runCmd(s, e, c) })

      clients.findById(clientId) >>= (_.fold(NotFound())(run))
  }
}
