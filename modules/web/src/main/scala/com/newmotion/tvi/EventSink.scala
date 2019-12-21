package com.newmotion.tvi

import cats.data.Kleisli
import cats.effect.{Concurrent, Sync}
import com.newmotion.logging.FLogger
import fs2.concurrent.Queue

// a every abstract algebra of an EventSink
trait EventSink[F[_], A] {
  def put(event: A): F[Unit]
}

// implementation could be...
abstract class KafkaEventSync[F[_], A]( /*producer: kafka.Producer*/ ) extends EventSink[F, A]

class LocalEventSink[F[_]: Sync, A](
  queue: Queue[F, A],
  logger: FLogger[F]
) extends EventSink[F, A] {
  import cats.syntax.apply._

  override def put(event: A): F[Unit] =
    logger.info(s"enqueueing ${event.toString}") *> queue.enqueue1(event)
}

object LocalEventSink {

  def impl[F[_]: Sync, A](
    queue: Queue[F, A]
  ): EventSink[F, A] =
    new LocalEventSink[F, A](
      queue,
      FLogger.fromClassUnsafe[F, LocalEventSink[F, A]]
    )
}

class LocalEventConsumer[F[_], A](
  numWorkers: Int,
  maxOpen: Int,
  q: Queue[F, A],
  w: A => F[Unit]
)(implicit F: Concurrent[F]) {
  import cats.implicits._
  import fs2._

  private def workerFun: fs2.Stream[F, Unit] =
    q.dequeue.evalMap(w) *> Stream.suspend(workerFun)

  def start: Stream[F, Unit] =
    Stream
      .range(0, numWorkers)
      .map(_ => workerFun)
      .covary[F]
      .parJoin(numWorkers / 2)
}

object LocalEventConsumer {

  def fromK[F[_]: Concurrent, A](
    k: Kleisli[F, A, Unit],
    id: String,
    queue: Queue[F, A],
    numWorkers: Int,
    maxOpen: Int
  ): LocalEventConsumer[F, A] =
    new LocalEventConsumer(
      numWorkers,
      maxOpen,
      queue,
      decorateK(k, FLogger.fromNameUnsafe(id)).run
    )

  import cats.syntax.applicativeError._
  import cats.syntax.apply._
  import cats.syntax.functor._

  private def decorateK[F[_], A, B](
    k: Kleisli[F, A, Unit],
    logger: FLogger[F]
  )(implicit F: Sync[F]): Kleisli[F, A, Unit] = {
    val prior = Kleisli[F, A, A](v => logger.info(s"received ${v}") *> F.pure(v))
    val after = Kleisli[F, A, Unit] { a =>
      k.run(a).handleErrorWith { e =>
        logger.error(e)("an error occurred").void
      }
    }
    prior.andThen(after)
  }
}
