package com.newmotion.tvi.tariff

import java.time.Instant

import com.newmotion.tvi.Repository.Criteria

trait TariffRepository[F[_]] {
  def findById(id: Long): F[Option[Tariff]]
  def findByStart(start: Instant): F[Option[Tariff]]
  def findWithin(start: Instant, end: Instant): F[List[Tariff]]
  def findAll(c: Criteria): F[List[Tariff]]
  def update(tariff: Tariff): F[Unit]
  def drop(id: Long): F[Unit]
  def :+(tariff: Tariff): F[Tariff]
}
