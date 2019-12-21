package com.newmotion.tvi.session

import java.time.Instant

import com.newmotion.tvi.tariff.Tariff

trait SessionRepository[F[_]] {
  def findById(id: Long): F[Option[Session]]

  def findByClientId(
    clientId: Long,
    offset: Int,
    limit: Int
  ): F[List[Session]]

  def findByChargePoint(
    chargePointId: Long,
    start: Instant,
    end: Instant
  ): F[Option[Session]]

  def findByClient(
    clientId: Long,
    start: Instant,
    end: Instant
  ): F[List[Session]]

  def :+(session: Session): F[Session]
}
