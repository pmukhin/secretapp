package com.newmotion.tvi.validation

import java.time.Instant

import cats.data.{Validated, ValidatedNel}

object instant {

  def startBeforeEnd(s: Instant, e: Instant): ValidatedNel[String, (Instant, Instant)] =
    (if (s.isAfter(e)) Validated.Invalid("start is after end")
     else if (s.equals(e)) Validated.Invalid("start is equal to end")
     else Validated.valid((s, e))).toValidatedNel

  def endIsInPast(e: Instant): ValidatedNel[String, Instant] =
    (if (e.isAfter(Instant.now)) Validated.invalid("end is not in the past")
     else Validated.valid(e)).toValidatedNel
}
