package com.newmotion.tvi

import java.time.ZonedDateTime

import cats.data.{Validated, ValidatedNel}
import org.http4s.dsl.impl.OptionalQueryParamDecoderMatcher
import org.http4s.{ParseFailure, QueryParamDecoder, QueryParameterValue}

object decoders {
  case class Limit(toInt: Int)                                  extends AnyVal
  case class Offset(toInt: Int)                                 extends AnyVal
  case class IncludeDeleted(toBool: Boolean)                    extends AnyVal
  case class ZonedDateTimeStart(toZonedDateTime: ZonedDateTime) extends AnyVal
  case class ZonedDateTimeEnd(toZonedDateTime: ZonedDateTime)   extends AnyVal

  private val limit   = "limit"
  private val offset  = "offset"
  private val deleted = "deleted"

  private def queryDecodedFailed(s: String)          = s"Query decoding $s failed"
  private def eToFailure(e: Throwable, f: String)    = sToFailure(e.getMessage, f)
  private def sToFailure(e: String, f: String)       = ParseFailure(queryDecodedFailed(f), e)
  private def beInRange(f: String, v0: Int, v1: Int) = s"$f should be in range [$v0..$v1]"
  private def beAtLeast(f: String, v0: Int)          = s"$f should be at least $v0"

  // noinspection TypeAnnotation
  implicit val limitQueryParamDecoder =
    new QueryParamDecoder[Limit] {

      override def decode(value: QueryParameterValue): ValidatedNel[ParseFailure, Limit] =
        Validated
          .catchNonFatal(value.value.toInt)
          .leftMap(eToFailure(_, limit))
          .andThen { v =>
            if (v > 0 || v < 100) Validated.valid(Limit(v))
            else Validated.Invalid(sToFailure(beInRange(limit, 1, 100), limit))
          }
          .toValidatedNel
    }

  // noinspection TypeAnnotation
  implicit val offsetQueryParamDecoder =
    new QueryParamDecoder[Offset] {

      override def decode(value: QueryParameterValue): ValidatedNel[ParseFailure, Offset] =
        Validated
          .catchNonFatal(value.value.toInt)
          .leftMap(eToFailure(_, offset))
          .andThen { v =>
            if (v >= 0) Validated.valid(Offset(v))
            else Validated.Invalid(sToFailure(beAtLeast(offset, 0), offset))
          }
          .toValidatedNel
    }

  // noinspection TypeAnnotation
  implicit val deleteQueryParamDecoder =
    QueryParamDecoder[Boolean].map(IncludeDeleted)

  object OptOffsetParam  extends OptionalQueryParamDecoderMatcher[Offset](offset)
  object OptLimitParam   extends OptionalQueryParamDecoderMatcher[Limit](limit)
  object OptDeletedParam extends OptionalQueryParamDecoderMatcher[IncludeDeleted](deleted)
}
