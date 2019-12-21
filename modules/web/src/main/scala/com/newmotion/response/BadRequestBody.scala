package com.newmotion.response

import cats.effect.Sync
import io.circe.Encoder
import io.circe.generic.semiauto._
import org.http4s.EntityEncoder
import org.http4s.circe.jsonEncoderOf

object BadRequestBody {

  implicit val encoder: Encoder[BadRequestBody] =
    deriveEncoder

  implicit def entityEncode[F[_]](
    implicit F: Sync[F]
  ): EntityEncoder[F, BadRequestBody] =
    jsonEncoderOf[F, BadRequestBody]
}

case class BadRequestBody(errors: List[String]) extends AnyVal
