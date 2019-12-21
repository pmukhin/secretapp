package com.newmotion

import java.time.{Instant, LocalDateTime, ZoneOffset}

import io.getquill.MappedEncoding

object encoders {

  implicit val encodeInstant: MappedEncoding[Instant, LocalDateTime] =
    MappedEncoding[Instant, LocalDateTime](LocalDateTime.ofInstant(_, ZoneOffset.UTC))

  implicit val decodeInstant: MappedEncoding[LocalDateTime, Instant] =
    MappedEncoding[LocalDateTime, Instant](_.toInstant(ZoneOffset.UTC))
}
