package com.newmotion.tvi.client.invoice

import java.time.{Instant, ZoneOffset}
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

import cats.effect.Sync
import com.newmotion.tvi.client.Client
import com.newmotion.tvi.client.invoice.Invoice.ChargedSession
import csv2s.Encoder
import fs2.Chunk
import org.http4s.{EntityEncoder, MediaType, headers}
import scala.language.implicitConversions

private[invoice] object responseEncoders {
  private val dispositionType = "attachment"
  private val filenameKey     = "filename"

  import csv2s.implicits._

  object formatDuration {

    private def toHours(duration: Long): String = {
      val hours = duration / TimeUnit.HOURS.toSeconds(1)
      if (hours > 0)
        s"${hours}h" + toMinutes(duration - TimeUnit.HOURS.toSeconds(hours))
      else toMinutes(duration)
    }

    private def toMinutes(duration: Long): String = {
      val minutes = duration / TimeUnit.MINUTES.toSeconds(1)
      if (minutes > 0)
        s"${minutes}m" + toSeconds(duration - TimeUnit.MINUTES.toSeconds(minutes))
      else toSeconds(duration)
    }

    private def toSeconds(duration: Long): String =
      if (duration > 0) s"${duration}s" else ""

    def apply(duration: Long): String = toHours(duration)

    def fromInstants(s: Instant, e: Instant): String =
      apply((e.toEpochMilli - s.toEpochMilli) / 1000)
  }

  def roundDouble(d: Double): Double =
    BigDecimal(d).setScale(2, BigDecimal.RoundingMode.HALF_UP).toDouble

  private val instantFormat =
    DateTimeFormatter
      .ofPattern("yyyy-MM-dd HH:mm:ss")
      .withZone(ZoneOffset.UTC)

  implicit val csvEncoderForSession: Encoder[ChargedSession] =
    Encoder.simple[ChargedSession] { cs =>
      instantFormat.format(cs.session.start) ::
        instantFormat.format(cs.session.end) ::
        formatDuration.fromInstants(cs.session.start, cs.session.end) ::
        roundDouble(cs.tariff.hourlyRate) ::
        cs.tariff.hourlyParkingRate.map(roundDouble).getOrElse(0.0) ::
        roundDouble(cs.totalEnergy) ::
        roundDouble(cs.totalParking) ::
        roundDouble(cs.totalEnergy + cs.totalParking) ::
        Nil
    }

  private val csvHeader =
    List(
      "start",
      "end",
      "duration",
      "rate",
      "parkingRate",
      "totalElectricity",
      "totalParking",
      "total"
    )

  implicit def csvEntityEncoder[F[_]: Sync](name: String): EntityEncoder[F, Invoice] =
    EntityEncoder.simple(
      headers.`Content-Type`(MediaType.text.`csv`),
      headers.`Content-Disposition`(dispositionType, Map(filenameKey -> name))
    )(i => Chunk.byteBuffer(i.sessions.asCsv.copy(header = Some(csvHeader)).toByteBuf))

  object fileNameFromClient {

    private def replaceSpaces(s: String) =
      s.replaceAll("[:\\s]", "_")

    def apply(c: Client, s: Instant, e: Instant): String =
      s"Invoice_${replaceSpaces(c.firstName)}_${replaceSpaces(c.lastName)}_" +
        s"${replaceSpaces(instantFormat.format(s))}_${replaceSpaces(instantFormat.format(e))}"
  }
}
