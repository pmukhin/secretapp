package com.newmotion.tvi.client

import java.time.LocalDate

import com.newmotion.tvi.client.Client.CreateCommand
import eu.timepit.refined.api.Refined
import eu.timepit.refined.collection.NonEmpty
import io.circe.generic.semiauto._
import io.circe.{Decoder, Encoder}
import io.scalaland.chimney.Transformer

object ClientRequest {
  import io.circe.refined._ // don't delete even if Intellij wants you to

  implicit val decoder: Decoder[ClientRequest] = deriveDecoder
  implicit val encoder: Encoder[ClientRequest] = deriveEncoder

  implicit def transformer: Transformer[ClientRequest, CreateCommand] =
    (src: ClientRequest) => CreateCommand(src.firstName.value, src.lastName.value, src.dob)
}

case class ClientRequest(
  firstName: Refined[String, NonEmpty],
  lastName: Refined[String, NonEmpty],
  dob: LocalDate
)
