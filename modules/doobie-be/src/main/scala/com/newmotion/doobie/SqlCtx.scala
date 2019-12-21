package com.newmotion.doobie

import java.time.{Instant, LocalDate}

import doobie.quill.DoobieContext.MySQL
import io.getquill.CamelCase
import io.getquill.context.sql.SqlContext

object SqlCtx {

  trait Quotes {
    this: SqlContext[_, _] =>

    // noinspection TypeAnnotation
    implicit class TimestampQuotes(left: Instant) {
      def >(right: Instant)  = quote(infix"$left > $right".as[Boolean])
      def <(right: Instant)  = quote(infix"$left < $right".as[Boolean])
      def >=(right: Instant) = quote(infix"$left >= $right".as[Boolean])
      def <=(right: Instant) = quote(infix"$left <= $right".as[Boolean])
      def ==(right: Instant) = quote(infix"$left = $right".as[Boolean])
    }

    // noinspection TypeAnnotation
    implicit class LocalDateQuotes(left: LocalDate) {
      def >(right: LocalDate)  = quote(infix"$left > $right".as[Boolean])
      def <(right: LocalDate)  = quote(infix"$left < $right".as[Boolean])
      def >=(right: LocalDate) = quote(infix"$left >= $right".as[Boolean])
      def <=(right: LocalDate) = quote(infix"$left <= $right".as[Boolean])
    }
  }

  val dc = new MySQL(CamelCase) with Quotes
}
