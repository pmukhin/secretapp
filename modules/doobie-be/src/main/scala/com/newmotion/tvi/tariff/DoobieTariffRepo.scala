package com.newmotion.tvi.tariff

import java.time.Instant

import cats.effect.Sync
import com.newmotion.doobie.SqlCtx.dc
import com.newmotion.doobie.SqlCtx.dc._
import com.newmotion.encoders._
import com.newmotion.tvi.Repository
import doobie.util.transactor.Transactor

private object TariffQuery {

  def findWithin(start: Instant, end: Instant): dc.Quoted[dc.Query[Tariff]] =
    quote {
      query[Tariff]
        .filter(!_.isDeleted)
        .filter(_.start < lift(start))
        .sortBy(_.start)
        .take(1)
        .union(
          query[Tariff]
            .filter(_.start > lift(start))
            .filter(_.start <= lift(end))
            .filter(!_.isDeleted)
        )
    }

  def insert(tariff: Tariff): dc.Quoted[dc.ActionReturning[Tariff, Long]] =
    quote {
      query[Tariff]
        .insert(lift(tariff))
        .returningGenerated(_.id)
    }

  def findByStart(starts: Instant): dc.Quoted[dc.Query[Tariff]] =
    quote {
      query[Tariff]
        .filter(!_.isDeleted)
        .filter(_.start == lift(starts))
        .take(1)
    }

  def findAll(
    limit: Int,
    offset: Int,
    includeDeleted: Boolean
  ): dc.Quoted[dc.Query[Tariff]] =
    quote {
      query[Tariff]
        .filter(includeDeleted || _.isDeleted)
        .drop(offset)
        .take(limit)
    }

  def drop(id: Long): dc.Quoted[dc.Update[Tariff]] =
    quote {
      query[Tariff]
        .filter(_.id == lift(id))
        .filter(!_.isDeleted)
        .update(_.isDeleted -> true)
    }

  def findById(id: Long): dc.Quoted[dc.Query[Tariff]] =
    quote {
      query[Tariff]
        .filter(_.id == lift(id))
        .filter(!_.isDeleted)
        .take(1)
    }
}

class DoobieTariffRepo[F[_]: Sync](xa: Transactor[F]) extends TariffRepository[F] {
  import Repository._
  import doobie.implicits._

  override def findById(id: Long): F[Option[Tariff]] =
    run(TariffQuery.findById(id)).map(_.headOption).transact(xa)

  override def findByStart(starts: Instant): F[Option[Tariff]] =
    run(TariffQuery.findByStart(starts)).map(_.headOption).transact(xa)

  override def findAll(c: Repository.Criteria): F[List[Tariff]] =
    run(TariffQuery.findAll(c.limit, c.offset, c.includeDeleted)).transact(xa)

  import cats.syntax.functor._

  override def drop(id: Long): F[Unit] =
    run(TariffQuery.drop(id)).transact(xa).assertDeleted

  override def :+(tariff: Tariff): F[Tariff] =
    run(TariffQuery.insert(tariff)).transact(xa).map(tariff.copy(_))

  override def findWithin(start: Instant, end: Instant): F[List[Tariff]] =
    run(TariffQuery.findWithin(start, end)).transact(xa)
}

object DoobieTariffRepo {

  def impl[F[_]: Sync](xa: Transactor[F]) =
    new DoobieTariffRepo(xa)
}
