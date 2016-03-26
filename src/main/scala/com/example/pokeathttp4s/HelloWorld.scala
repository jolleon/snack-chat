package com.example.pokeathttp4s

import com.example.pokeathttp4s.models._
import org.http4s._
import org.http4s.dsl._

import _root_.argonaut._, Argonaut._
import org.http4s.argonaut._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scalaz.concurrent.Task

object HelloWorld {

  val service = HttpService {
    case req @ GET -> Root / "api" / "rooms" =>
      Ok(RoomsDAO.listAll().map(_.asJson))
    case req @ GET -> Root / "api" / "rooms" / roomId =>
      getRoomResponse(roomId)
    case req @ GET -> Root / "api" / "rooms" / roomId / "messages" =>
      Ok(MessagesDAO.forRoom(roomId.toLong).map(_.asJson))
    case req @ POST -> Root / "api" / "rooms" =>
      req.decode[RoomInput] { r =>
        Util.futureToTask(RoomsDAO.create(r)) flatMap { room =>
          Ok(room.asJson)
        }
      }
    case req @ POST -> Root / "api" / "rooms" / roomId / "messages" =>
      req.decode[MessageInput] { m =>
        Util.futureToTask(MessagesDAO.postToRoom(roomId.toLong, m)) flatMap { message =>
          Ok(message.asJson)
        }
      }
    case req @ GET -> Root =>
      StaticFile.fromResource("/index.html", Some(req)).fold(NotFound())(Task.now)
    case req @ GET -> "static" /: path =>
      StaticFile.fromResource("/static" + path, Some(req)).fold(NotFound())(Task.now)
  }

  def getRoomResponse(roomId: String): Task[Response] = {
    Util.futureToTask(getRoom(roomId.toLong)) flatMap  {
      case Some((room, messages)) =>
        Ok(room.asJson.->:(("messages", messages.asJson)))
      case None => NotFound()
    }
  }

  def getRoom(roomId: Long): Future[Option[(Room, Seq[models.Message])]] = {
    val rF = RoomsDAO.getById(roomId)
    val mF = MessagesDAO.forRoom(roomId)
    for {
      room <- rF
      messages <- mF
    } yield room map { (_, messages) }
  }
}
