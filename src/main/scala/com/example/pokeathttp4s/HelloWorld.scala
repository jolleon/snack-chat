package com.example.pokeathttp4s

import com.example.pokeathttp4s.models.{Room, MessagesDAO, MessageInput, RoomsDAO}
import org.http4s._
import org.http4s.dsl._

import _root_.argonaut._, Argonaut._
import org.http4s.argonaut._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scalaz.concurrent.Task

object HelloWorld {

  val service = HttpService {
    case GET -> Root / "rooms" =>
      Ok(RoomsDAO.listAll().map(_.asJson))
    case GET -> Root / "rooms" / roomId =>
      getRoomResponse(roomId)
    case req @ POST -> Root / "rooms" / roomId =>
      req.decode[MessageInput] { m =>
        Util.futureToTask(MessagesDAO.postToRoom(roomId.toLong, m)) flatMap { message =>
          Ok(message.asJson)
        }
      }
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
