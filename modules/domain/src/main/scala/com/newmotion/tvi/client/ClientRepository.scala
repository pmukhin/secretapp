package com.newmotion.tvi.client

import com.newmotion.tvi.Repository.Criteria

trait ClientRepository[F[_]] {
  def findById(id: Long): F[Option[Client]]
  def findAll(c: Criteria): F[List[Client]]
  def drop(id: Long): F[Unit]
  def :+(client: Client): F[Client]
}
