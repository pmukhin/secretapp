package com.newmotion.logging

import cats.effect.Sync

import scala.reflect.ClassTag

object FLogger {

  def fromClass[F[_]: Sync, A](implicit C: ClassTag[A]): F[FLogger[F]] =
    Sync[F].delay(fromClassUnsafe[F, A])

  def fromClassUnsafe[F[_]: Sync, A](implicit C: ClassTag[A]): FLogger[F] =
    FLogger(org.log4s.getLogger(C.runtimeClass))

  def fromName[F[_]: Sync, A](name: String): F[FLogger[F]] =
    Sync[F].delay(fromNameUnsafe(name))

  def fromNameUnsafe[F[_]: Sync, A](name: String): FLogger[F] =
    FLogger(org.log4s.getLogger(name))
}

case class FLogger[F[_]](logger: org.log4s.Logger)(implicit F: Sync[F]) extends AnyRef {
  def info(t: Throwable)(msg: String): F[Unit]  = F.delay(logger.info(t)(msg))
  def info(msg: String): F[Unit]                = F.delay(logger.info(msg))
  def error(msg: String): F[Unit]               = F.delay(logger.error(msg))
  def error(e: Throwable)(msg: String): F[Unit] = F.delay(logger.error(e)(msg))
  def trace(t: Throwable)(msg: String): F[Unit] = F.delay(logger.trace(t)(msg))
  def trace(msg: String): F[Unit]               = F.delay(logger.trace(msg))
  def debug(t: Throwable)(msg: String): F[Unit] = F.delay(logger.debug(t)(msg))
  def debug(msg: String): F[Unit]               = F.delay(logger.debug(msg))
  def warn(t: Throwable)(msg: String): F[Unit]  = F.delay(logger.warn(t)(msg))
  def warn(msg: String): F[Unit]                = F.delay(logger.warn(msg))
}
