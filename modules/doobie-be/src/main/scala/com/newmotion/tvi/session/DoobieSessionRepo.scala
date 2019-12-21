package com.newmotion.tvi.session

import java.time.Instant

import cats.effect.Sync
import com.newmotion.doobie.SqlCtx.dc
import com.newmotion.doobie.SqlCtx.dc._
import doobie._
import com.newmotion.encoders._ // don't remove it

object SessionQueries {
  import com.newmotion.tvi.session

  def findByClient(
    clientId: Long,
    start: Instant,
    end: Instant
  ): dc.Quoted[dc.Query[Session]] =
    quote {
      query[session.Session]
        .filter(_.clientId == lift(clientId))
        .filter(_.start >= lift(start))
        .filter(_.end < lift(end))
        .sortBy(_.start)(Ord.asc)
    }

  def findById(id: Long): dc.Quoted[dc.Query[session.Session]] =
    quote {
      query[session.Session]
        .filter(_.id == lift(id))
        .take(1)
    }

  def findByClientId(
    clientId: Long,
    offset: Int,
    limit: Int
  ): dc.Quoted[dc.Query[session.Session]] =
    quote {
      query[session.Session]
        .filter(_.clientId == lift(clientId))
        .drop(lift(offset))
        .take(lift(limit))
    }

  def findByChargePoint(
    chargePointId: Long,
    start: Instant,
    end: Instant
  ): dc.Quoted[dc.Query[session.Session]] =
    quote {
      query[session.Session]
        .filter(_.chargePointId == lift(chargePointId))
        .filter(s => s.start >= lift(start) && s.end <= lift(end))
    }

  def insert(s: session.Session): dc.Quoted[dc.ActionReturning[session.Session, Long]] =
    quote {
      query[session.Session]
        .insert(lift(s))
        .onConflictIgnore
        .returningGenerated(_.id)
    }
}

class DoobieSessionRepo[F[_]: Sync](xa: Transactor[F]) extends SessionRepository[F] {
  import doobie.implicits._

  override def findById(id: Long): F[Option[Session]] =
    run(SessionQueries.findById(id)).map(_.headOption).transact[F](xa)

  override def findByClientId(
    clientId: Long,
    offset: Int,
    limit: Int
  ): F[List[Session]] =
    run(SessionQueries.findByClientId(clientId, offset, limit)).transact[F](xa)

  override def findByChargePoint(
    chargePointId: Long,
    start: Instant,
    end: Instant
  ): F[Option[Session]] =
    run(SessionQueries.findByChargePoint(chargePointId, start, end))
      .map(_.headOption)
      .transact(xa)

  override def findByClient(
    clientId: Long,
    start: Instant,
    end: Instant
  ): F[List[Session]] =
    run(SessionQueries.findByClient(clientId, start, end)).transact(xa)

  import cats.syntax.functor._

  override def :+(session: Session): F[Session] =
    run(SessionQueries.insert(session))
      .transact(xa)
      .map(session.copy(_))
}

object DoobieSessionRepo {

  def impl[F[_]: Sync](xa: Transactor[F]): SessionRepository[F] =
    new DoobieSessionRepo(xa)
}
