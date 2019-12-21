package com.newmotion.tvi.session

import cats.effect.Sync
import com.newmotion.logging.FLogger
import com.newmotion.tvi.EventSink
import com.newmotion.response.BadRequestBody
import org.http4s.circe.{jsonEncoderOf, jsonOf}
import org.http4s.dsl.Http4sDsl
import org.http4s.{EntityDecoder, EntityEncoder, HttpRoutes}

object SessionRoutes {

  implicit def sessionEncoder[F[_]: Sync]: EntityEncoder[F, Session] =
    jsonEncoderOf[F, Session]

  implicit def sesReqDecoder[F[_]: Sync]: EntityDecoder[F, SessionRequest] =
    jsonOf[F, SessionRequest]

  def impl[F[_]: Sync](
    sessions: SessionRepository[F],
    eventSync: EventSink[F, Session.CreateCommand]
  ): SessionRoutes[F] =
    new SessionRoutes(
      sessions,
      FLogger.fromClassUnsafe[F, SessionRoutes[F]],
      eventSync
    )
}

class SessionRoutes[F[_]: Sync](
  sessions: SessionRepository[F],
  logger: FLogger[F],
  eventSync: EventSink[F, Session.CreateCommand]
) extends Http4sDsl[F] {
  import SessionRequest._
  import SessionRoutes._
  import cats.syntax.applicativeError._
  import cats.syntax.flatMap._
  import com.newmotion.tvi.helpers._

  val routes: HttpRoutes[F] = HttpRoutes.of[F] {
    case GET -> Root / LongVar(id) =>
      sessions.findById(id).orNotFound

    // here I assume that creating sessions is the most
    // frequent operation I need to support. So the idea
    // is to use a back-pressure algorithm. Instead of
    // performing writes synchronously I put write requests
    // to a queue, from which multiple workers read and
    // perform further actions
    case req @ POST -> Root =>
      val enqueue = req.as[SessionRequest] >>=
        toCreateCommandK.run >>=
        eventSync.put

      // let's give some info on validation errors
      enqueue.andOk_.handleErrorWith {
        case StartIsAfterEnd =>
          BadRequest(BadRequestBody("start is after end" :: Nil))
        case EmptyInterval =>
          BadRequest(BadRequestBody("interval is empty: start == end" :: Nil))
      }
  }
}
