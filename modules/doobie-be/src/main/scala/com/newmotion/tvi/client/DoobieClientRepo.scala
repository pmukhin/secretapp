package com.newmotion.tvi.client

import cats.effect.Sync
import cats.syntax.functor._
import com.newmotion.tvi.Repository
import com.newmotion.doobie.SqlCtx.dc
import dc._
import doobie.util.transactor.Transactor

object ClientQueries {

  def insert(client: Client): dc.Quoted[dc.ActionReturning[Client, Long]] =
    quote {
      query[Client]
        .insert(lift(client))
        .returningGenerated(_.id)
    }

  def drop(id: Long): dc.Quoted[dc.Update[Client]] =
    quote {
      query[Client]
        .filter(_.id == lift(id))
        .update(_.isDeleted -> lift(true))
    }

  def findAll(
    limit: Int,
    offset: Int,
    includeDeleted: Boolean
  ): dc.Quoted[dc.Query[Client]] =
    quote {
      query[Client]
        .filter(c => if (includeDeleted) true else !c.isDeleted)
        .drop(offset)
        .take(limit)
    }

  def findById(id: Long): dc.Quoted[dc.Query[Client]] =
    quote {
      query[Client]
        .filter(_.id == lift(id))
        .filter(!_.isDeleted)
        .take(1)
    }
}

class DoobieClientRepo[F[_]: Sync](xa: Transactor[F]) extends ClientRepository[F] {
  import doobie.implicits._
  import Repository._

  override def findById(id: Long): F[Option[Client]] =
    run(ClientQueries.findById(id)).map(_.headOption).transact(xa)

  override def findAll(c: Repository.Criteria): F[List[Client]] =
    run(ClientQueries.findAll(c.limit, c.offset, c.includeDeleted)).transact(xa)

  override def drop(id: Long): F[Unit] =
    run(ClientQueries.drop(id)).transact(xa).assertDeleted

  override def :+(client: Client): F[Client] =
    run(ClientQueries.insert(client)).transact(xa).map(client.copy(_))
}

object DoobieClientRepo {

  def impl[F[_]: Sync](xa: Transactor[F]) =
    new DoobieClientRepo(xa)
}
