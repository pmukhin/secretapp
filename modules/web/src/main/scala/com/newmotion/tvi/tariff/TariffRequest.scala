package com.newmotion.tvi.tariff

import java.time.ZonedDateTime

import com.newmotion.tvi.formats
import eu.timepit.refined.api.Refined
import eu.timepit.refined.numeric.Positive
import io.circe.Decoder
import io.circe.generic.semiauto.deriveDecoder
import io.scalaland.chimney.Transformer

import scala.util.Try

object TariffRequest {
  import cats.syntax.either._
  import io.circe.refined._

  implicit lazy val zonedDateTimeDecoder: Decoder[ZonedDateTime] =
    Decoder.decodeString.emap { s =>
      Try(ZonedDateTime.parse(s, formats.zonedDateTimeFormat)).toEither
        .leftMap(_.toString)
    }

  implicit val decoder: Decoder[TariffRequest] = deriveDecoder

  // oops, looks like chimney can't handle this case
  implicit val t: Transformer[TariffRequest, Tariff.CreateCommand] =
    (src: TariffRequest) =>
      Tariff.CreateCommand(
        src.hourlyRate.value,
        src.hourlyParkingRate.map(_.value),
        src.start.toInstant
      )

  // oops, looks like chimney can't handle this case
  import scala.language.implicitConversions // for this little case
  implicit def trans(id: Long): Transformer[TariffRequest, Tariff.UpdateCommand] =
    (src: TariffRequest) =>
      Tariff.UpdateCommand(
        id,
        src.hourlyRate.value,
        src.hourlyParkingRate.map(_.value),
        src.start.toInstant
      )
}

// using ZonedDateTime explicitly to accept zoned
// time to be later converted and stored in UTC
case class TariffRequest(
  hourlyRate: Refined[Double, Positive],
  hourlyParkingRate: Option[Refined[Double, Positive]],
  start: ZonedDateTime
)
