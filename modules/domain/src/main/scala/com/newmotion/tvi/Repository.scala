package com.newmotion.tvi

import cats.effect.Sync

object Repository {

  case class Criteria(
    limit: Int,
    offset: Int,
    includeDeleted: Boolean
  )

  case object AlreadyDeleted extends RuntimeException

  implicit class FOps[F[_]: Sync](val x: F[Long]) {
    import cats.syntax.applicative._

    def assertDeleted: F[Unit] =
      Sync[F].flatMap(x)(d => Sync[F].raiseError(AlreadyDeleted).whenA(d != 0))
  }
}
