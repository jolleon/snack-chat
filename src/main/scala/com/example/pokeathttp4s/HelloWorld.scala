package com.example.pokeathttp4s

import com.example.pokeathttp4s.models.{Room, Message, MessagesDAO, RoomsDAO}
import org.http4s._
import org.http4s.dsl._

import _root_.argonaut._, Argonaut._
import org.http4s.argonaut._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Await, Future}
import scalaz.concurrent.Task
import scala.concurrent.duration._


object HelloWorld {


  val service = HttpService {
    case GET -> Root / "rooms" =>
      Ok(RoomsDAO.listAll().map(_.asJson))
    case GET -> Root / "rooms" / roomId =>
      Await.result(getRoomResponse(roomId), 2.seconds) // this is sad - need to figure how to use http4s with Futures
  }

  def getRoomResponse(roomId: String): Future[Task[Response]] = {
    getRoom(roomId.toLong) map {
      case Some((room, messages)) =>
        Ok(jObjectFields(
          ("name", room.name.asJson),
          ("messages", messages.asJson)
        ))
      case None => NotFound()
    }
  }

  def getRoom(roomId: Long): Future[Option[(Room, Seq[Message])]] = {
    val rF = RoomsDAO.getById(roomId)
    val mF = MessagesDAO.forRoom(roomId)
    for {
      room <- rF
      messages <- mF
    } yield room map { (_, messages) }
  }
}
