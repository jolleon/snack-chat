package com.example.pokeathttp4s.models

import org.joda.time.DateTime
import slick.driver.PostgresDriver.api._
import argonaut._, Argonaut._
import scala.concurrent.Future


case class Message(id: Long, roomId: Long, author: String, content: String, created: DateTime)

// JSON converter so we can use the case class directly in the API
object Message {
  implicit def MessageCodecJson: CodecJson[Message] =
    casecodec5(Message.apply, Message.unapply)("id", "roomId", "author", "content", "created")
}


class Messages(tag: Tag) extends Table[Message](tag, "MESSAGES") {
  def id = column[Long]("id", O.PrimaryKey)
  def roomId = column[Long]("roomId")
  def author = column[String]("author")
  def content = column[String]("content")
  def created = column[DateTime]("created")

  def * = (id, roomId, author, content, created) <> ((Message.apply _).tupled, Message.unapply)
  def room = foreignKey("ROOM_FK", roomId, RoomsDAO.rooms)(_.id)
}

object MessagesDAO {
  val messages = TableQuery[Messages]

  def forRoom(roomId: Long): Future[Seq[Message]] = {
    val q = messages.filter(_.roomId === roomId)
    db.run(q.result)
  }

}