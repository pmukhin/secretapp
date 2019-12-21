package com.newmotion.tvi

import cats.Applicative
import cats.effect.Sync
import org.http4s.{EntityEncoder, Response}
import org.http4s.dsl.Http4sDsl
import cats.implicits._

object helpers {

  implicit class HttpFOptionOps[F[_], A](
    val fa: F[Option[A]]
  )(implicit F: Sync[F])
      extends Http4sDsl[F] {

    def orNotFound(implicit ev: EntityEncoder[F, A]): F[Response[F]] =
      fa >>= (_.fold(NotFound())(Ok(_)))

    // if fa resolves to None NotFound (404) is returned, else Ok (200) is
    // returned after some ops are performed
    def orNotFoundAndThen_(ops: F[_]): F[Response[F]] =
      fa >>= (_.fold(NotFound())(_ => ops >>= (_ => Ok())))

    def orNotFoundAndThen_(ops: A => F[_]): F[Response[F]] =
      fa >>= (_.fold(NotFound())(ops(_) >>= (_ => Ok())))
  }

  implicit class HttpFUnitOps[F[_]: Sync](val fu: F[Unit]) extends Http4sDsl[F] {
    // noinspection TypeAnnotation
    val andOk_ = fu >>= (_ => Ok())
  }

  implicit class HttpFAOps[F[_]: Sync, A](val fa: F[A]) extends Http4sDsl[F] {

    def andOk(implicit ev: EntityEncoder[F, A]): F[Response[F]] =
      fa.flatMap(Ok(_))
  }
}
