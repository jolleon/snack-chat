package com.example.pokeathttp4s

import com.example.pokeathttp4s.models.{MessagesDAO, RoomsDAO}
import org.http4s._
import org.http4s.dsl._

import _root_.argonaut._, Argonaut._
import org.http4s.argonaut._
import scala.concurrent.ExecutionContext.Implicits.global


object HelloWorld {


  val service = HttpService {
    case GET -> Root / "hello" / name =>
      Ok(jSingleObject("message", jString(s"Hello, $name")))
    case GET -> Root / "db" =>
      Ok(RoomsDAO.listAll().map(_.asJson))
    case GET -> Root / "rooms" / roomId =>
      Ok(MessagesDAO.forRoom(roomId.toLong).map(_.asJson))
  }
}
