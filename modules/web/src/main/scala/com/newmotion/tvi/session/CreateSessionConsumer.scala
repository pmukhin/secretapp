package com.newmotion.tvi.session

import cats.data.Kleisli
import cats.effect.Concurrent
import com.newmotion.tvi.LocalEventConsumer
import fs2.concurrent.Queue

object CreateSessionConsumer {
  // just an alias
  type Ev = Session.CreateCommand

  def fromK[F[_]: Concurrent](
    workerK: Kleisli[F, Ev, Unit],
    queue: Queue[F, Session.CreateCommand],
    numWorkers: Int
  ): LocalEventConsumer[F, Ev] =
    LocalEventConsumer.fromK(
      workerK,
      "create-session-consumer",
      queue,
      numWorkers,
      numWorkers / 2
    )
}
